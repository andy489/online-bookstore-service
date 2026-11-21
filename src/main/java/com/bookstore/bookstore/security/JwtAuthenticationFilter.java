package com.bookstore.bookstore.security;

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

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenBlocklistService tokenBlocklistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlocklistService = tokenBlocklistService;
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

        String jti = claims.getId();
        if (tokenBlocklistService.isBlocked(jti)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = claims.getSubject();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtTokenProvider.isClaimsValid(claims)) {
                Instant tokenIssuedAt = claims.getIssuedAt().toInstant();
                Long tvaEpochSec = claims.get("tva", Long.class);
                Instant tokenValidAfter = tvaEpochSec != null
                        ? Instant.ofEpochSecond(tvaEpochSec) : Instant.EPOCH;

                // Allow tokens issued at or after the threshold set at login time
                if (tokenIssuedAt.compareTo(tokenValidAfter) >= 0) {
                    String role = claims.get("role", String.class);
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
