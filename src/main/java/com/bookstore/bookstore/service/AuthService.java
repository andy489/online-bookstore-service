package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.Role;
import com.bookstore.bookstore.domain.User;
import com.bookstore.bookstore.dto.request.LoginRequest;
import com.bookstore.bookstore.dto.request.RegisterRequest;
import com.bookstore.bookstore.dto.response.AuthResponse;
import com.bookstore.bookstore.exception.DuplicateResourceException;
import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                Role.USER
        );
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}
