package com.syfe.finance.category;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        @JsonProperty("isCustom")
        boolean custom
) {
}
