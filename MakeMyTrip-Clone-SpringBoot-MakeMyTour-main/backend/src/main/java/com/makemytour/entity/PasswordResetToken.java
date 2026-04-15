package com.makemytour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One-time token stored when a user requests a password reset.
 * Tokens expire after 1 hour and are deleted once used.
 */
@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Secure random UUID sent to the user's e-mail */
    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** UTC instant after which the token is no longer valid (1 hour) */
    @Column(nullable = false)
    private Instant expiresAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
