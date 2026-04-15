package com.makemytour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Application user – stores credentials and roles for Spring Security.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password – never stored in plain text */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phoneNumber;

    /**
     * User's preferred UI language (e.g., "en", "hi", "fr").
     * Persisted here so the same language is applied across devices.
     * Feature 3 – Multi-Language Support.
     */
    @Builder.Default
    private String preferredLanguage = "en";

    /**
     * Whether the user prefers OLED dark mode.
     * Persisted here so dark mode state is synced across devices.
     * Feature 5 – OLED-Optimized Dark Mode.
     */
    @Builder.Default
    private boolean darkMode = false;

    /** Roles like ROLE_USER, ROLE_ADMIN */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>();
}
