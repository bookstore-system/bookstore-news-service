package com.notfound.newsservice.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContextUnitTest {

    @Mock
    HttpServletRequest request;

    UserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = new UserContext(request);
    }

    @Test
    void isAdmin_acceptsRoleAdmin() {
        when(request.getHeader(UserContext.HEADER_USER_ROLE)).thenReturn("ROLE_ADMIN");
        assertTrue(userContext.isAdmin());
    }

    @Test
    void isAdmin_acceptsAdminFromGateway() {
        when(request.getHeader(UserContext.HEADER_USER_ROLE)).thenReturn("ADMIN");
        assertTrue(userContext.isAdmin());
    }

    @Test
    void isAdmin_rejectsUserRole() {
        when(request.getHeader(UserContext.HEADER_USER_ROLE)).thenReturn("ROLE_USER");
        assertFalse(userContext.isAdmin());
    }
}
