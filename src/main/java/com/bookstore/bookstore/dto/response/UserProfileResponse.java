package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.User;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
