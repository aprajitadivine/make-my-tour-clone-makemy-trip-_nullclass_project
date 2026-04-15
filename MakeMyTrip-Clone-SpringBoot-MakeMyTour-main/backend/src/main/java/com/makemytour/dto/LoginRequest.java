package com.makemytour.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/auth/login – accepts username OR e-mail */
@Data
public class LoginRequest {

    /** Can be a username or an e-mail address */
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}
