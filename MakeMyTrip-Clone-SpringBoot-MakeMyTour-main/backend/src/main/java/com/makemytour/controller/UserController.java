package com.makemytour.controller;

import com.makemytour.dto.UpdateProfileRequest;
import com.makemytour.entity.User;
import com.makemytour.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API for user profile management and preferences.
 * Covers Feature 3 (language preference) and Feature 5 (dark mode preference).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * GET /api/users/me – Returns the current user's full profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = loadCurrentUser(userDetails);
        return ResponseEntity.ok(buildProfileMap(user));
    }

    /**
     * PATCH /api/users/me/profile – Updates fullName and/or phoneNumber.
     */
    @PatchMapping("/me/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest body) {
        User user = loadCurrentUser(userDetails);
        if (body.getFullName() != null && !body.getFullName().isBlank()) {
            user.setFullName(body.getFullName().trim());
        }
        if (body.getPhoneNumber() != null && !body.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(body.getPhoneNumber().trim());
        }
        userRepository.save(user);
        return ResponseEntity.ok(buildProfileMap(user));
    }

    /**
     * PATCH /api/users/me/language – Persists the user's language preference.
     * Feature 3 – Multi-Language Support.
     * Request body: { "language": "hi" }
     */
    @PatchMapping("/me/language")
    public ResponseEntity<?> updateLanguage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String lang = body.get("language");
        if (lang == null || lang.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "language field is required"));
        }
        User user = loadCurrentUser(userDetails);
        user.setPreferredLanguage(lang.trim());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("preferredLanguage", lang.trim(), "message", "Language preference saved"));
    }

    /**
     * PATCH /api/users/me/theme – Persists the user's dark mode preference.
     * Feature 5 – OLED-Optimized Dark Mode.
     * Request body: { "darkMode": true }
     */
    @PatchMapping("/me/theme")
    public ResponseEntity<?> updateTheme(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Boolean> body) {
        Boolean darkMode = body.get("darkMode");
        if (darkMode == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "darkMode field is required"));
        }
        User user = loadCurrentUser(userDetails);
        user.setDarkMode(darkMode);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("darkMode", darkMode, "message", "Theme preference saved"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User loadCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private Map<String, Object> buildProfileMap(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id",                user.getId());
        profile.put("username",          user.getUsername());
        profile.put("email",             user.getEmail());
        profile.put("fullName",          user.getFullName());
        profile.put("phoneNumber",       user.getPhoneNumber());
        profile.put("preferredLanguage", user.getPreferredLanguage());
        profile.put("darkMode",          user.isDarkMode());
        profile.put("roles",             user.getRoles());
        return profile;
    }
}
