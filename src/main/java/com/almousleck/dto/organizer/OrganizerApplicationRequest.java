package com.almousleck.dto.organizer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizerApplicationRequest {

    private String description;

    @NotBlank(message = "Company name cannot be blank")
    private String companyName;

    private String website;
    private String socialMediaLinks;
}

