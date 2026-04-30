# bookstore-news-service

Microservice quản lý tin tức (news) cho hệ thống Bookstore. Hỗ trợ CRUD, search nâng cao, thống kê, view counter, upload nhiều ảnh qua **Cloudinary**, parse HTML metadata bằng **Jsoup**.

## 1. Tech Stack

| Hạng mục | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.0.5, Spring Cloud 2025.1.1 |
| Datastore | MySQL 8.0 (DB `bookstore_news`) — Database per Service |
| Image storage | Cloudinary (`cloudinary-http44` 1.39.0) |
| HTML parser | Jsoup 1.17.2 |
| ORM | Spring Data JPA + Hibernate |
| Validation | Bean Validation |
| Build | Maven 3.9+ |
| Container | Docker, image `truongikpk/bookstore-news-service:<tag>` |

## 2. Cổng & quy ước

| Thành phần | Host port | Container port |
| --- | --- | --- |
| `news-service` | **8089** | 8080 |
| `news-db` (MySQL) | **3309** (chỉ ở docker-compose của service) | 3306 |
| `api-gateway` | 8080 | 8080 |

> Mọi request từ client đi qua API Gateway. Gateway xác thực JWT, sau đó forward 3 header:
> - `X-User-Id` (UUID)
> - `X-User-Name` (String) — tuỳ chọn (sẽ được lưu vào `news.author_name`)
> - `X-User-Role` (`ROLE_USER` hoặc `ROLE_ADMIN`)

## 3. Biến môi trường

| Biến | Mặc định (local) | Mặc định (docker) | Mô tả |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | _(none)_ | `docker` | profile cấu hình |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/bookstore_news?...` | `jdbc:mysql://news-db:3306/bookstore_news?...` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `bookstore` | `bookstore` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `bookstore` | `bookstore` | DB password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | `update` | Hibernate auto DDL |
| `CLOUDINARY_CLOUD_NAME` | _(empty)_ | _(empty)_ | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | _(empty)_ | _(empty)_ | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | _(empty)_ | _(empty)_ | Cloudinary API secret |

> Khi không có Cloudinary, upload ảnh trả URL placeholder (`https://placehold.co/...`) — vẫn tạo được news, chỉ ảnh là placeholder.

## 4. Yêu cầu môi trường

- JDK 21
- Maven 3.9+ (project có Maven Wrapper)
- Docker Desktop / Docker Engine
- MySQL 8.0 chạy ở `localhost:3306` (nếu chạy local thuần)
- Tài khoản Cloudinary (tuỳ chọn) — xem https://cloudinary.com

## 5. Cách chạy

### 5.1. Chạy local trực tiếp với Maven

```powershell
# 1. Khởi động MySQL (nếu chưa có)
docker run -d --name news-db-local -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=bookstore_news `
  -e MYSQL_USER=bookstore -e MYSQL_PASSWORD=bookstore `
  mysql:8.0

# 2. (tuỳ chọn) Set Cloudinary
$env:CLOUDINARY_CLOUD_NAME = "demo"
$env:CLOUDINARY_API_KEY    = "...."
$env:CLOUDINARY_API_SECRET = "...."

# 3. Build & chạy news-service
cd mircoservice/bookstore-news-service
./mvnw spring-boot:run
```

### 5.2. Chạy bằng `docker-compose.yml` ngay trong thư mục service

```powershell
cd mircoservice/bookstore-news-service
docker network create bookstore-network 2>$null
docker compose up -d
docker compose logs -f news-service
```

Service truy cập tại `http://localhost:8089`, MySQL nội bộ tại `localhost:3309`.

### 5.3. Chạy toàn bộ hệ thống với `docker-compose.dev.yml`

```powershell
cd mircoservice/bookstore-news-service
$env:CLOUDINARY_CLOUD_NAME = "your_cloud"
$env:CLOUDINARY_API_KEY    = "your_key"
$env:CLOUDINARY_API_SECRET = "your_secret"
docker compose -f docker-compose.dev.yml up -d
```

File này chạy đầy đủ infra: `user-db`, `book-db`, `news-db`, `cart-redis`, `rabbitmq`, `api-gateway`, các service (image trên Docker Hub) + `frontend`.

## 6. Curl test mẫu

### 6.1. Endpoint công khai (Public)

```bash
# UUID mẫu dùng cho curl examples:
#   USER_ID=11111111-1111-1111-1111-111111111111
#   NEWS_ID=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa

# 1) Lấy danh sách news (paging + sort)
curl "http://localhost:8089/api/news?page=0&size=10&sort=createdAt&order=desc"

# 2) Chỉ news đã PUBLISHED
curl "http://localhost:8089/api/news/published?page=0&size=10"

# 3) Chi tiết 1 news (đồng thời tăng views)
curl http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa

# 4) Search theo title
curl "http://localhost:8089/api/news/search?title=spring"

# 5) Lấy theo tác giả
curl http://localhost:8089/api/news/author/11111111-1111-1111-1111-111111111111

# 6) Lấy theo category
curl http://localhost:8089/api/news/category/Tech

# 7) Lấy theo tag (full-text-like)
curl http://localhost:8089/api/news/tag/spring-boot

# 8) Advanced search (kết hợp keyword + category + status)
curl "http://localhost:8089/api/news/advanced-search?keyword=spring&category=Tech&status=PUBLISHED"

# 9) Đếm theo trạng thái (public)
curl http://localhost:8089/api/news/stats/count
```

### 6.2. Endpoint admin/owner (cần header `X-User-Role: ROLE_ADMIN`)

```bash
# 10) Tạo tin
curl -X POST http://localhost:8089/api/news \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Name: Admin" -H "X-User-Role: ROLE_ADMIN" \
  -d '{
        "title": "Spring Boot 4 phát hành",
        "summary": "Bản phát hành quan trọng",
        "content": "<h2>Tổng quan</h2><p>Nội dung...</p>",
        "category": "Tech",
        "tags": ["spring-boot", "java"],
        "featured": true,
        "status": "PUBLISHED"
      }'

# 11) Cập nhật
curl -X PUT http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN" \
  -d '{"title":"Spring Boot 4 - bản cập nhật","status":"PUBLISHED"}'

# 12) Publish / Archive / Restore
curl -X PUT http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/publish \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"
curl -X PUT http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/archive \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"
curl -X PUT http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/restore \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"

# 13) Xoá
curl -X DELETE http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"

# 14) Thống kê (chỉ admin)
curl http://localhost:8089/api/news/statistics \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"

# 15) My News (theo X-User-Id)
curl http://localhost:8089/api/news/my-news \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"
```

### 6.3. Upload ảnh

```bash
# 16) Upload nhiều ảnh cho 1 news
curl -X POST http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/images \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN" \
  -F "images=@./pic1.jpg" -F "images=@./pic2.jpg"

# 17) Xoá 1 ảnh khỏi news
curl -X DELETE http://localhost:8089/api/news/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/images/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -H "X-User-Role: ROLE_ADMIN"
```

### 6.4. Đi qua API Gateway

Thay `localhost:8089` bằng `localhost:8080` và thay header `X-User-Id`/`X-User-Role` bằng `Authorization: Bearer <JWT>`. Gateway sẽ verify token và forward 3 header trên về service.

## 7. Định dạng response

```json
{
  "code": 200,
  "message": "Tạo tin tức thành công",
  "result": {
    "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "title": "Spring Boot 4",
    "summary": "...",
    "content": "<h2 id=\"section-1\">Tổng quan</h2><p>...</p>",
    "metadata": {
      "description": "Nội dung...",
      "sections": [
        { "id": "section-1", "title": "Tổng quan", "level": 2 }
      ],
      "links": []
    },
    "status": "PUBLISHED",
    "category": "Tech",
    "tags": ["spring-boot", "java"],
    "views": 0,
    "featured": true,
    "createdAt": "2026-04-30 06:00:00",
    "updatedAt": "2026-04-30 06:00:00",
    "publishedAt": "2026-04-30 06:00:00",
    "authorId": "11111111-1111-1111-1111-111111111111",
    "authorName": "Admin",
    "images": []
  }
}
```

## 8. Mã lỗi & xử lý

| HTTP | Mã | Khi nào xảy ra |
| --- | --- | --- |
| 200 | 200 | Thành công |
| 400 | 400 | Validation lỗi (title rỗng, content rỗng, category rỗng) |
| 401 | 401 | Thiếu header `X-User-Id` ở endpoint cần auth |
| 403 | 403 | Endpoint admin nhưng `X-User-Role` không phải `ROLE_ADMIN` |
| 404 | 404 | News id không tồn tại |
| 413 | 413 | File upload > 10MB |
| 500 | 500 | Lỗi không lường trước |

## 9. Troubleshooting

| Triệu chứng | Nguyên nhân | Khắc phục |
| --- | --- | --- |
| Không kết nối được DB | MySQL chưa khởi động | `docker compose up -d news-db` rồi đợi `healthcheck` |
| Upload ảnh trả URL `placehold.co` | Cloudinary chưa cấu hình | Set `CLOUDINARY_*` env vars rồi restart |
| Lỗi `MaxUploadSizeExceededException` | File > 10MB | Tăng `spring.servlet.multipart.max-file-size` trong `application.yaml` |
| `getNewsById` luôn 404 | DB rỗng / sai bookstore_news | Tạo dữ liệu mẫu hoặc kiểm tra DDL_AUTO=update |
| FE bắn 403 cho create | Gateway không gắn header `ROLE_ADMIN` | Kiểm tra JWT claims; user phải có role admin |

## 10. Build & push image

```powershell
cd mircoservice/bookstore-news-service
./push.ps1 v1.0.0
```

## 11. Cấu trúc package

```
com.notfound.newsservice
├── BookstoreNewsServiceApplication
├── config/CloudinaryConfig
├── controller/NewsController
├── exception/{GlobalExceptionHandler, *Exception}
├── model/
│   ├── converter/StringListConverter
│   ├── dto/{request, response}/*
│   ├── entity/{News, NewsImage}
│   └── enums/NewsStatus
├── repository/{NewsRepository, NewsImageRepository}
├── service/
│   ├── NewsService, ImageService
│   └── impl/{NewsServiceImpl, CloudinaryImageServiceImpl}
└── util/UserContext
```

## 12. Kiến trúc luồng

```
Frontend → API Gateway (8080) ──[X-User-Id, X-User-Name, X-User-Role]──► news-service (8089)
                                                                            │
                                                                            ├──► news-db (MySQL 3309/3306) JPA
                                                                            └──► Cloudinary (HTTPS)
```

## 13. Smoke test

```powershell
./mvnw test
```

Bao gồm:
- `NewsControllerWebMvcTest` — kiểm tra `GET /api/news` & `/published` qua MockMvc.
- `BookstoreNewsServiceApplicationTests.contextLoads` — boot context với H2 in-memory (RabbitMQ auto-config đã exclude).
