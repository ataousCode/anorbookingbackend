package com.almousleck.dto.organizer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrganizerProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String companyName;
    private String description;
    private String website;
    private List<String> socialMediaLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
