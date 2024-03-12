package com.programmingtechie.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programmingtechie.productservice.dto.ProductRequest;
import com.programmingtechie.productservice.dto.ProductResponse;
import com.programmingtechie.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.2.2");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProductRepository productRepository;

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry){
		dynamicPropertyRegistry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
	}

	@Test
	void shouldCreateProduct() throws Exception {

		productRepository.deleteAll();

		ProductRequest productRequest = getProductRequest();
		String productRequestString = objectMapper.writeValueAsString(productRequest);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequestString))
				.andExpect(status().isCreated());
		Assertions.assertEquals(1,productRepository.findAll().size());
	}

	@Test
	void shouldGetProduct() throws Exception {
		shouldCreateProduct();
		ProductResponse productResponse = getProductResponse();
		mockMvc.perform(MockMvcRequestBuilders.get("/api/product"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].description").value(productResponse.getDescription()))
				.andExpect(jsonPath("$[0].name").value(productResponse.getName()))
				.andExpect(jsonPath("$[0].price").value(productResponse.getPrice()));
	}

	private ProductResponse getProductResponse() {
		return ProductResponse.builder()
				.description("Iphone14 Description")
				.name("Iphone14")
				.price(BigDecimal.valueOf(1500))
				.build();
	}

	private ProductRequest getProductRequest() {
		return ProductRequest.builder()
				.description("Iphone14 Description")
				.name("Iphone14")
				.price(BigDecimal.valueOf(1500))
				.build();
	}

}
