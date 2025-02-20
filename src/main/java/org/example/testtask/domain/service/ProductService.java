package org.example.testtask.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.testtask.domain.model.Product;
import org.example.testtask.infrastructure.cache.RedisProductCache;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final RedisProductCache redisProductCache;

    /**
     * Завантажує продукти за списком ID з Redis.
     */
    public Flux<Product> getProductsByIds(Flux<String> productIds) {
        return productIds.flatMap(this::getProductById)
                .doOnNext(product -> log.info("Product loaded: {}", product.getProductName()));
    }

    /**
     * Завантажує один продукт за його ID з Redis.
     */
    public Mono<Product> getProductById(String productId) {
        return Mono.justOrEmpty(redisProductCache.getProduct(productId))
                .switchIfEmpty(Mono.defer(() -> loadProductFromCacheOrFallback(productId)))
                .doOnNext(product -> {
                    if ("Missing Product Name".equals(product.getProductName())) {
                        log.warn("Product not found for ID: {}", productId);
                    }
                });
    }

    /**
     * Завантажує продукт з кешу.
     */
    private Mono<Product> loadProductFromCacheOrFallback(String productId) {
        Product fallbackProduct = new Product(productId, "Missing Product Name");
        return Mono.justOrEmpty(redisProductCache.getProduct(productId))
                .switchIfEmpty(Mono.just(fallbackProduct));
    }

    public Mono<Void> loadProducts(Flux<Product> products) {
        return products
                .doOnNext(product -> log.info("Processing product: {}", product.getProductName()))
                .map(product -> {
                    redisProductCache.saveProduct(product);
                    return product;
                })
                .doOnComplete(() -> log.info("All products have been loaded into cache"))
                .doOnError(error -> log.error("Error loading products into cache: {}", error.getMessage()))
                .then();
    }

}
