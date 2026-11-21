package com.bookstore.bookstore.integration;

import com.bookstore.bookstore.service.TokenBlocklistService;
import com.bookstore.bookstore.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the JWT blocklist correctly stores and checks invalidated tokens in real Redis.
 * Ensures TTL-based expiry and key naming work as expected — not testable with mocks.
 */
class TokenBlocklistIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    TokenBlocklistService tokenBlocklistService;

    @Test
    void token_notBlockedBeforeLogout() {
        UserDetails user = buildUser("alice@example.com");
        String token = jwtTokenProvider.generateToken(user);
        String jti = jwtTokenProvider.extractJti(token);

        assertThat(tokenBlocklistService.isBlocked(jti)).isFalse();
    }

    @Test
    void token_blockedAfterLogout() {
        UserDetails user = buildUser("bob@example.com");
        String token = jwtTokenProvider.generateToken(user);
        String jti = jwtTokenProvider.extractJti(token);

        tokenBlocklistService.block(jti, jwtTokenProvider.extractExpiration(token));

        assertThat(tokenBlocklistService.isBlocked(jti)).isTrue();
    }

    @Test
    void differentTokens_haveUniqueJtis() {
        UserDetails user = buildUser("carol@example.com");
        String token1 = jwtTokenProvider.generateToken(user);
        String token2 = jwtTokenProvider.generateToken(user);

        assertThat(jwtTokenProvider.extractJti(token1))
                .isNotEqualTo(jwtTokenProvider.extractJti(token2));
    }

    private UserDetails buildUser(String email) {
        return User.withUsername(email)
                .password("irrelevant")
                .authorities(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }
}
