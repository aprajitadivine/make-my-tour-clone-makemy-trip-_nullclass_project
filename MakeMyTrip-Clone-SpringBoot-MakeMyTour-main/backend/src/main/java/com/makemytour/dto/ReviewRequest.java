package com.makemytour.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request body for POST /api/reviews */
@Data
public class ReviewRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    /** Star rating – must be between 1 and 5 */
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    /** Optional URL to an uploaded image (screenshot / photo) */
    private String imageUrl;
}
