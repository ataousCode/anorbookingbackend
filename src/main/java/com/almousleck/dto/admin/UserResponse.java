package com.almousleck.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phoneNumber;
    private boolean enabled;
    private List<String> roles;
    private LocalDateTime createdAt;
}
