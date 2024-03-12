package com.programmingtechie.inventoryservice.service;

import com.programmingtechie.inventoryservice.dto.InventoryResponse;
import com.programmingtechie.inventoryservice.model.Inventory;
import com.programmingtechie.inventoryservice.repository.InventoryRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
//    @SneakyThrows
    public List<InventoryResponse> isInStock(List<String> skuCodes){
//        log.info("Wait started");
//        Thread.sleep(10000);
//        log.info("Wait ended");
        log.info("Request received from OS {}", skuCodes);
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(inventory -> matToInventoryDto(inventory))
                .toList();

    }

    private InventoryResponse matToInventoryDto(Inventory inventory) {
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() > 0)
                .build();
    }
}
