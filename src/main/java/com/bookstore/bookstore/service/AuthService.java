package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.Role;
import com.bookstore.bookstore.domain.User;
import com.bookstore.bookstore.dto.request.LoginRequest;
import com.bookstore.bookstore.dto.request.RegisterRequest;
import com.bookstore.bookstore.dto.response.AuthResponse;
import com.bookstore.bookstore.exception.DuplicateResourceException;
import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenBlocklistService tokenBlocklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager,
                       TokenBlocklistService tokenBlocklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.tokenBlocklistService = tokenBlocklistService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already in use: " + email);
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                Role.USER
        );
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(
                org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities("ROLE_" + user.getRole().name())
                        .build(),
                user.getTokenValidAfter()
        );
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        java.time.Instant tokenValidAfter = userRepository.findByEmail(userDetails.getUsername())
                .map(User::getTokenValidAfter)
                .orElse(java.time.Instant.EPOCH);
        String token = jwtTokenProvider.generateToken(userDetails, tokenValidAfter);
        String role = userDetails.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
        return new AuthResponse(token, userDetails.getUsername(), role);
    }

    public void logout(String token) {
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        tokenBlocklistService.block(claims.getId(), claims.getExpiration());
    }
}
