package com.makemytour.controller;

import com.makemytour.dto.ForgotPasswordRequest;
import com.makemytour.dto.JwtResponse;
import com.makemytour.dto.LoginRequest;
import com.makemytour.dto.RegisterRequest;
import com.makemytour.dto.ResetPasswordRequest;
import com.makemytour.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for user registration, login, and password-reset flow.
 * All endpoints are publicly accessible (no authentication required).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * POST /api/auth/register
     * Creates a new user account.
     *
     * <p>Error body format: {@code { "error": "<message>", "code": "<CODE>" }}
     * <ul>
     *   <li>{@code EMAIL_EXISTS}    – e-mail already registered; frontend should redirect to /login</li>
     *   <li>{@code USERNAME_EXISTS} – username taken; ask user to pick another</li>
     *   <li>{@code VALIDATION_ERROR} – request body failed bean-validation</li>
     * </ul>
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Account created successfully. You can now log in."));
        } catch (IllegalArgumentException e) {
            return buildCodedError(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Registration failed. Please try again.", "code", "SERVER_ERROR"));
        }
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    /**
     * POST /api/auth/login
     * Authenticates a user (username OR e-mail) and returns a JWT.
     *
     * <p>Error body format: {@code { "error": "<message>", "code": "<CODE>" }}
     * <ul>
     *   <li>{@code USER_NOT_FOUND}      – no account found; frontend should redirect to /register</li>
     *   <li>{@code INVALID_CREDENTIALS} – wrong password</li>
     * </ul>
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            JwtResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            return buildCodedError(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "error", "Incorrect password. Please try again.",
                        "code",  "INVALID_CREDENTIALS"));
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed. Please try again.", "code", "AUTH_ERROR"));
        }
    }

    // ------------------------------------------------------------------
    // Forgot / Reset password
    // ------------------------------------------------------------------

    /**
     * POST /api/auth/forgot-password
     * Sends a password-reset link to the supplied e-mail (if registered).
     * Always returns HTTP 200 to avoid leaking whether the e-mail exists.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
        } catch (Exception e) {
            log.error("Error processing forgot-password request", e);
        }
        // Always return 200 to prevent e-mail enumeration
        return ResponseEntity.ok(Map.of(
            "message", "If that e-mail is registered you will receive a password-reset link shortly."));
    }

    /**
     * POST /api/auth/reset-password
     * Validates the token and sets a new password.
     *
     * <p>Error codes:
     * <ul>
     *   <li>{@code INVALID_TOKEN} – token not found or already used</li>
     *   <li>{@code TOKEN_EXPIRED} – token has expired; user must request a new one</li>
     * </ul>
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
        } catch (IllegalArgumentException e) {
            return buildCodedError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error during password reset", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Password reset failed. Please try again.", "code", "SERVER_ERROR"));
        }
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /**
     * Parses messages in the format {@code "CODE:Human readable message"} into a
     * JSON body with separate {@code code} and {@code error} fields.
     */
    private ResponseEntity<Map<String, String>> buildCodedError(String rawMessage, HttpStatus status) {
        String code  = "ERROR";
        String error = rawMessage;
        if (rawMessage != null && rawMessage.contains(":")) {
            int idx = rawMessage.indexOf(':');
            code  = rawMessage.substring(0, idx);
            error = rawMessage.substring(idx + 1);
        }
        return ResponseEntity.status(status).body(Map.of("error", error, "code", code));
    }
}
