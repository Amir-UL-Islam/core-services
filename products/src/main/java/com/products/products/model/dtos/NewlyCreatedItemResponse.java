package com.products.products.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewlyCreatedItemResponse {
    private String name;
    private String description;
    private Long price;
}
