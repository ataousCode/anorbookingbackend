package com.almousleck.dto.organizer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizerApplicationStatusRequest {

    @NotBlank(message = "Status cannot be blank")
    private String status;

    private String adminFeedback;
}
