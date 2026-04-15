package com.makemytour.service;

import com.makemytour.entity.PasswordResetToken;
import com.makemytour.entity.User;
import com.makemytour.dto.ForgotPasswordRequest;
import com.makemytour.dto.JwtResponse;
import com.makemytour.dto.LoginRequest;
import com.makemytour.dto.RegisterRequest;
import com.makemytour.dto.ResetPasswordRequest;
import com.makemytour.repository.PasswordResetTokenRepository;
import com.makemytour.repository.UserRepository;
import com.makemytour.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/**
 * Handles user registration, JWT-based login, and password-reset flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final long RESET_TOKEN_EXPIRY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new user account.
     *
     * <p>Error codes in thrown {@link IllegalArgumentException}:
     * <ul>
     *   <li>{@code EMAIL_EXISTS}    – e-mail already registered → frontend should redirect to login</li>
     *   <li>{@code USERNAME_EXISTS} – username already taken → ask user to choose another</li>
     * </ul>
     *
     * @param request validated registration data
     * @return the persisted {@link User}
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "EMAIL_EXISTS:This e-mail is already registered. Please login instead.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                "USERNAME_EXISTS:Username '" + request.getUsername()
                + "' is already taken. Please choose a different username.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of("ROLE_USER"))
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getUsername());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user by username OR e-mail and returns a signed JWT.
     *
     * <p>Error codes propagated via exceptions:
     * <ul>
     *   <li>{@link UsernameNotFoundException} – no account found → frontend should redirect to register</li>
     *   <li>{@link BadCredentialsException}   – account found but wrong password</li>
     * </ul>
     *
     * @param request login credentials (username or e-mail + password)
     * @return {@link JwtResponse} with the signed token and user details
     */
    public JwtResponse login(LoginRequest request) {
        // Resolve the actual username, whether the user typed a username or an e-mail
        String username = resolveUsername(request.getUsernameOrEmail());

        // Delegate credential validation to Spring Security (throws on bad credentials)
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        log.info("User logged in: {}", username);

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    // -------------------------------------------------------------------------
    // Forgot / Reset password
    // -------------------------------------------------------------------------

    /**
     * Generates a one-time reset token and e-mails it to the user.
     *
     * <p>The response is always a generic success message to avoid leaking whether
     * an e-mail address is registered (security best practice).
     *
     * @param request contains the user's e-mail address
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidate any previous token for this user
            resetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(Instant.now().plus(RESET_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                    .build();
            resetTokenRepository.save(resetToken);

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            } catch (IllegalStateException ex) {
                log.error("Could not dispatch reset email for user {}: {}", user.getUsername(), ex.getMessage());
                // Do not propagate – we still respond with the generic message below
            }
        });
        // Always log at a level that doesn't reveal existence
        log.info("Password-reset flow triggered for e-mail (existence not disclosed): {}", email);
    }

    /**
     * Validates a reset token and sets the new password.
     *
     * @param request token + new password
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException(
                    "INVALID_TOKEN:Password reset link is invalid or has already been used."));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException(
                "TOKEN_EXPIRED:Password reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Consume the token so it cannot be reused
        resetTokenRepository.delete(resetToken);
        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * If {@code usernameOrEmail} contains '@' it is treated as an e-mail and the
     * corresponding username is looked up. Otherwise it is used as-is.
     *
     * @throws UsernameNotFoundException when no account matches the supplied identifier
     */
    private String resolveUsername(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail.trim().toLowerCase())
                    .map(User::getUsername)
                    .orElseThrow(() -> new UsernameNotFoundException(
                        "USER_NOT_FOUND:No account found with this e-mail. Please register first."));
        }
        // Verify the username exists before attempting authentication to give a clearer error
        if (!userRepository.existsByUsername(usernameOrEmail)) {
            throw new UsernameNotFoundException(
                "USER_NOT_FOUND:No account found with this username. Please register first.");
        }
        return usernameOrEmail;
    }
}
