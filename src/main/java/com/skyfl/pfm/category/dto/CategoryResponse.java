package com.skyfl.pfm.category.dto;

import com.skyfl.pfm.category.entity.CategoryType;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String name,
        CategoryType type,
        String icon,
        String color,
        boolean system
) {
}
