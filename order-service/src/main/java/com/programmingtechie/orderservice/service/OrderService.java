package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.event.OrderPlacedEvent;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItem;
import com.programmingtechie.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, WebClient.Builder webClient, KafkaTemplate kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClient;
        this.kafkaTemplate = kafkaTemplate;
    }


    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItem> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(orderLineItemsDto -> mapToDto(orderLineItemsDto, order))
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        // Call inventory service and place order if product is in stock

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(orderLineItem -> orderLineItem.getSkuCode())
                .toList();

        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                            .uri("http://inventory-service/api/inventory",
                                    uriBuilder -> uriBuilder.queryParam("skuCodes", skuCodes).build())
                            .retrieve()
                            .bodyToMono(InventoryResponse[].class)
                            .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(inventoryResponse -> inventoryResponse.isInStock());

        log.info("Response received from is {}", allProductsInStock);
        if(allProductsInStock){
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            return "Order placed successfully";
        } else {
            throw new IllegalStateException("Product not in stock, please try again later");
        }

    }

    private OrderLineItem mapToDto(OrderLineItemsDto orderLineItemsDto, Order order){
        return OrderLineItem.builder()
                .skuCode(orderLineItemsDto.getSkuCode())
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .order(order)
                .build();
    }

}
