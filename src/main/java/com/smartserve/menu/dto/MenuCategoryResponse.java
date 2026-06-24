package com.smartserve.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class MenuCategoryResponse {

    private Long id;

    private String name;

    private String description;

    private Integer displayOrder;

    private Boolean active;

    private Instant createdAt;

    private Instant updatedAt;
}
