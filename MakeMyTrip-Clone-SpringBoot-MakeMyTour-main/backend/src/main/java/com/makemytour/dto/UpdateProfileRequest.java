package com.makemytour.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for PATCH /api/users/me/profile */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
}
