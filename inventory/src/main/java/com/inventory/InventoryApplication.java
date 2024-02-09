package com.inventory;

import com.inventory.model.Inventory;
import com.inventory.repository.InventoryRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }


    @Bean
    public CommandLineRunner loadInventoryDate(InventoryRepository inventoryRepository) {
        return args -> {
            for (int i = 0; i < 9; i++) {
                Inventory inventory = new Inventory();
                inventory.setSkuCode(RandomStringUtils.randomAlphabetic(5));
                inventory.setQuantity(10L + i);
                inventory.setUnitePrice(100L + i * 10);
                inventoryRepository.save(inventory);
            }
        };
    }

}
