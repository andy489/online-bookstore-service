package com.bookstore.bookstore.security;

import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.service.TokenBlocklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlocklistService tokenBlocklistService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenBlocklistService tokenBlocklistService,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlocklistService = tokenBlocklistService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            // Single parse + HMAC verify — all subsequent reads use the returned Claims object
            claims = jwtTokenProvider.extractAllClaims(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bug #6 fix: null jti must be treated as blocked (fail-closed)
        String jti = claims.getId();
        if (jti == null || tokenBlocklistService.isBlocked(jti)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = claims.getSubject();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtTokenProvider.isClaimsValid(claims)) {
                // Bug #1 fix: compare token iat against tokenValidAfter from DB, not from token
                Instant tokenIssuedAt = claims.getIssuedAt().toInstant();
                Instant tokenValidAfter = userRepository.findByEmail(username.toLowerCase())
                        .map(u -> u.getTokenValidAfter() != null ? u.getTokenValidAfter() : Instant.EPOCH)
                        .orElse(Instant.MAX); // unknown user → reject

                if (tokenIssuedAt.compareTo(tokenValidAfter) >= 0) {
                    String role = claims.get("role", String.class);
                    if (role == null || role.isBlank()) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    var authorities = List.of(new SimpleGrantedAuthority(role));
                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
