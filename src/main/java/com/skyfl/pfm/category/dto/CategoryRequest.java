package com.skyfl.pfm.category.dto;

import com.skyfl.pfm.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryRequest(
        UUID parentId,
        @NotBlank @Size(max = 100) String name,
        @NotNull CategoryType type,
        @Size(max = 100) String icon,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "must be a HEX color") String color
) {
}
