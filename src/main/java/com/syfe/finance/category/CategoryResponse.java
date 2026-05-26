package com.syfe.finance.category;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryResponse {

    private final Long id;
    private final String name;
    private final CategoryType type;
    private final boolean custom;

    public CategoryResponse(Long id, String name, CategoryType type, boolean custom) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.custom = custom;
    }

    @JsonProperty("id")
    public Long id() {
        return id;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("type")
    public CategoryType type() {
        return type;
    }

    @JsonProperty("custom")
    public boolean custom() {
        return custom;
    }

    @JsonProperty("isCustom")
    public boolean isCustom() {
        return custom;
    }
}
