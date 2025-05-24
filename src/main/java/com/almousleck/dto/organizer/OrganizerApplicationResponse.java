package com.almousleck.dto.organizer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizerApplicationResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String description;
    private String companyName;
    private String website;
    private String socialMediaLinks;
    private String status;
    private String adminFeedback;
    private LocalDateTime createdAt;
}

