package org.example.testtask.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.model.Trade;
import org.example.testtask.infrastructure.parser.CsvParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TradeService {

    @Autowired
    private CsvParser csvParser;

    private final Map<String, Product> productCache = new ConcurrentHashMap<>();

    /**
     * Завантажує всі продукти та кешує їх.
     */
    @Cacheable("products")
    public Mono<Map<String, Product>> loadProducts(Flux<Product> products) {
        return products
                .collectMap(Product::getProductId, product -> product)
                .doOnNext(map -> {
                    productCache.clear();
                    productCache.putAll(map); // Оновлюємо кеш
                    log.info("Product cache updated: {}", productCache);
                });
    }

    /**
     * Збагачує трейд інформацією про продукт.
     */
    public Mono<Trade> enrichTradeWithProduct(Trade trade) {
        Product product = productCache.get(trade.getProductId());

        if (product == null) {
            log.warn("Product not found for productId: {}", trade.getProductId());
            return Mono.just(trade.withProductName("Missing Product Name"));
        }

        return Mono.just(trade.withProductName(product.getProductName())); // Використовуємо `withProductName`
    }


    /**
     * Обробляє CSV і збагачує кожен запис.
     */
    public Mono<Void> enrichTrades(Flux<Trade> trades) {
        return trades
                .flatMap(this::enrichTradeWithProduct)
                .doOnNext(enrichedTrade -> log.info("Enriched trade: {}", enrichedTrade))
                .then();
    }
}
