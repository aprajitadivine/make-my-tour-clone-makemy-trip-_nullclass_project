package com.makemytour.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a TravelStory.
 * Feature 4 – User-Generated Content & Community.
 */
@Data
public class TravelStoryRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String coverImageUrl;

    private String destination;
}
