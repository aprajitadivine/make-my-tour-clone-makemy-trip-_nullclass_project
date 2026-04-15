package com.makemytour.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for POST /api/auth/register */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username may only contain letters, digits, and underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    /**
     * Strong password rules:
     * – At least 8 characters
     * – At least one uppercase letter
     * – At least one lowercase letter
     * – At least one digit
     * – At least one special character from: @ $ ! % * ? & _ #
     */
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#])[A-Za-z\\d@$!%*?&_#]{8,}$",
        message = "Password must be at least 8 characters and contain uppercase, lowercase, digit, and special character (@$!%*?&_#)"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @Pattern(
        regexp = "^(\\+?[0-9][0-9\\-\\s]{6,19})?$",
        message = "Phone number must be 7-20 digits and may start with +"
    )
    private String phoneNumber;
}
