package com.almousleck.dto.user;


import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;

    private String phoneNumber;
}
