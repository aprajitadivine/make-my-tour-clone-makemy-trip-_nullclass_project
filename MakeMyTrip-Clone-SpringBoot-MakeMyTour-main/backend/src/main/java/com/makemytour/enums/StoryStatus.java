package com.makemytour.enums;

/**
 * Moderation status for user-generated Travel Stories.
 * Feature 4 – User-Generated Content & Community.
 */
public enum StoryStatus {
    /** Awaiting review by the moderation team */
    PENDING,
    /** Approved and visible to all users */
    APPROVED,
    /** Rejected – hidden from public view */
    REJECTED
}
