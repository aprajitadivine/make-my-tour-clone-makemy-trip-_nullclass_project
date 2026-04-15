package com.makemytour.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for submitting a multi-dimensional DetailedReview.
 * Feature 6 – Rate & Review System (RRS).
 */
@Data
public class DetailedReviewRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @Min(value = 1, message = "Punctuality score must be between 1 and 5")
    @Max(value = 5, message = "Punctuality score must be between 1 and 5")
    private int punctualityScore;

    @Min(value = 1, message = "Cleanliness score must be between 1 and 5")
    @Max(value = 5, message = "Cleanliness score must be between 1 and 5")
    private int cleanlinessScore;

    @Min(value = 1, message = "Amenities score must be between 1 and 5")
    @Max(value = 5, message = "Amenities score must be between 1 and 5")
    private int amenitiesScore;

    private String comment;
}
