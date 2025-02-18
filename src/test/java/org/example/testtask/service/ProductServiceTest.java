package org.example.testtask.service;


import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.service.ProductService;
import org.example.testtask.infrastructure.cache.RedisProductCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private RedisProductCache redisProductCache;

    @InjectMocks
    private ProductService productService;

    private Product validProduct;

    @BeforeEach
    void setUp() {
        validProduct = new Product("1", "test product");
    }

    @Test
    void shouldGetProductById() {
        // given
        when(redisProductCache.getProduct("1")).thenReturn(Optional.ofNullable(validProduct));

        // when
        Mono<Product> result = productService.getProductById("1");

        // then
        StepVerifier.create(result)
                .expectNextMatches(product ->
                        product.getProductId().equals("1") &&
                                product.getProductName().equals("test product"))
                .verifyComplete();
    }

    @Test
    void shouldReturnFallbackForMissingProduct() {
        when(redisProductCache.getProduct("999")).thenReturn(null);

        Mono<Product> result = productService.getProductById("999");

        StepVerifier.create(result)
                .expectNextMatches(product ->
                        product.getProductId().equals("999") &&
                                product.getProductName().equals("Missing Product Name"))
                .verifyComplete();
    }

    @Test
    void shouldGetProductsByIds() {
        when(redisProductCache.getProduct("1")).thenReturn(Optional.ofNullable(validProduct));
        when(redisProductCache.getProduct("2")).thenReturn(Optional.of(new Product("2", "test product 2")));

        Flux<Product> result = productService.getProductsByIds(Flux.just("1", "2"));

        StepVerifier.create(result)
                .expectNextMatches(product ->
                        product.getProductId().equals("1") &&
                                product.getProductName().equals("test product"))
                .expectNextMatches(product ->
                        product.getProductId().equals("2") &&
                                product.getProductName().equals("test product 2"))
                .verifyComplete();
    }

    @Test
    void shouldHandleMixOfValidAndInvalidProducts() {
        when(redisProductCache.getProduct("1")).thenReturn(Optional.ofNullable(validProduct));
        when(redisProductCache.getProduct("999")).thenReturn(null);

        Flux<Product> result = productService.getProductsByIds(Flux.just("1", "999"));

        StepVerifier.create(result)
                .expectNextMatches(product ->
                        product.getProductId().equals("1") &&
                                product.getProductName().equals("test product"))
                .expectNextMatches(product ->
                        product.getProductId().equals("999") &&
                                product.getProductName().equals("Missing Product Name"))
                .verifyComplete();
    }

    @Test
    void shouldLoadProducts() {
        Flux<Product> products = Flux.just(validProduct);

        Mono<Void> result = productService.loadProducts(products);

        StepVerifier.create(result)
                .verifyComplete();
    }
}