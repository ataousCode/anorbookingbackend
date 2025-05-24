package com.almousleck.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;
    private String iconUrl;
    private boolean active;
}
