package com.makemytour.dto;

import lombok.Data;

/** Optional request body for DELETE /api/bookings/{id}/cancel */
@Data
public class CancelRequest {

    /**
     * Reason chosen by the user from the dropdown.
     * Valid values: "Change of plans", "Found a better deal", "Medical emergency",
     *               "Work / Personal obligation", "Duplicate booking", "Other"
     */
    private String reason;
}
