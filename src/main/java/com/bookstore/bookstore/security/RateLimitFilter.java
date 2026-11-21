package com.bookstore.bookstore.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/users/me/password"
    );

    private static final Supplier<BucketConfiguration> BUCKET_CONFIG = () ->
            BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(5)
                            .refillIntervally(5, Duration.ofMinutes(1))
                            .build())
                    .build();

    private final ProxyManager<String> buckets;

    public RateLimitFilter(ProxyManager<String> rateLimitProxyManager) {
        this.buckets = rateLimitProxyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        if (!RATE_LIMITED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(h -> h.split(",")[0].trim())
                .orElse(request.getRemoteAddr());
        String bucketKey = "rate:" + path + ":" + ip;

        Bucket bucket = buckets.builder().build(bucketKey, BUCKET_CONFIG);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests — try again in 60 seconds\"}"
            );
        }
    }
}
