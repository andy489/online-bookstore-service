package com.bookstore.bookstore.integration;

import com.bookstore.bookstore.dto.request.LoginRequest;
import com.bookstore.bookstore.dto.request.RegisterRequest;
import com.bookstore.bookstore.dto.response.AuthResponse;
import com.bookstore.bookstore.exception.DuplicateResourceException;
import com.bookstore.bookstore.security.JwtTokenProvider;
import com.bookstore.bookstore.service.AuthService;
import com.bookstore.bookstore.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired TokenBlocklistService tokenBlocklistService;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void register_newUser_returnsTokenWithCorrectRole() {
        AuthResponse response = authService.register(registerRequest("auth-test-1@example.com"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("auth-test-1@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void register_tokenIsValid() {
        AuthResponse response = authService.register(registerRequest("auth-test-2@example.com"));

        String extractedEmail = jwtTokenProvider.extractUsername(response.getToken());
        assertThat(extractedEmail).isEqualTo("auth-test-2@example.com");
        assertThat(jwtTokenProvider.isTokenValid(response.getToken(),
                org.springframework.security.core.userdetails.User
                        .withUsername("auth-test-2@example.com")
                        .password("x").authorities(java.util.Collections.emptyList()).build()))
                .isTrue();
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResource() {
        authService.register(registerRequest("dup-auth@example.com"));

        assertThatThrownBy(() -> authService.register(registerRequest("dup-auth@example.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void login_correctCredentials_returnsToken() {
        authService.register(registerRequest("login-test@example.com"));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("login-test@example.com");
        loginReq.setPassword("password123");
        AuthResponse response = authService.login(loginReq);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("login-test@example.com");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        authService.register(registerRequest("bad-pass@example.com"));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("bad-pass@example.com");
        loginReq.setPassword("wrongpassword");

        assertThatThrownBy(() -> authService.login(loginReq))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsBadCredentials() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("nobody@nowhere.com");
        loginReq.setPassword("whatever");

        assertThatThrownBy(() -> authService.login(loginReq))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_blocksToken() {
        AuthResponse response = authService.register(registerRequest("logout-test@example.com"));
        String token = response.getToken();
        String jti = jwtTokenProvider.extractJti(token);

        assertThat(tokenBlocklistService.isBlocked(jti)).isFalse();

        authService.logout(token);

        assertThat(tokenBlocklistService.isBlocked(jti)).isTrue();
    }

    @Test
    void twoLogins_produceDifferentTokensWithDifferentJtis() {
        authService.register(registerRequest("unique-jti@example.com"));

        LoginRequest req = new LoginRequest();
        req.setEmail("unique-jti@example.com");
        req.setPassword("password123");

        String token1 = authService.login(req).getToken();
        String token2 = authService.login(req).getToken();

        assertThat(jwtTokenProvider.extractJti(token1))
                .isNotEqualTo(jwtTokenProvider.extractJti(token2));
    }

    private RegisterRequest registerRequest(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("password123");
        r.setFirstName("Test");
        r.setLastName("User");
        return r;
    }
}
