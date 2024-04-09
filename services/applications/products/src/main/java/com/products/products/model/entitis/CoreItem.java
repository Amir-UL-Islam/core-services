package com.products.products.model.entitis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("core_items") // Collection is more like table names in MySQL
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CoreItem {
    private String id;
    private String name;
    private String description;
    private Long price;
}
