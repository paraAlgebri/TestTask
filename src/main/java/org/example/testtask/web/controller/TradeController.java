package org.example.testtask.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.model.Trade;
import org.example.testtask.domain.service.ProductService;
import org.example.testtask.domain.service.TradeService;
import org.example.testtask.infrastructure.parser.CsvParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.StringReader;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TradeController {

    private final CsvParser csvParser;
    private final TradeService tradeService;
    private final ProductService productService;

    @PostMapping(value = "/enrich", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Flux<Trade> enrichTradeData(@RequestBody String csvData) {
        return Flux.using(
                () -> new StringReader(csvData),
                reader -> {
                    Flux<Trade> trades = csvParser.parseTrades(reader);
                    if (trades == null) {
                        log.error("CsvParser returned null instead of Flux<Trade>");
                        return Flux.empty();
                    }
                    return trades.flatMap(trade -> {
                        if (trade == null) {
                            log.error("Null trade encountered in stream");
                            return Mono.empty();
                        }
                        return tradeService.enrichTradeWithProduct(trade);
                    });
                },
                reader -> {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        log.error("Error closing reader", e);
                    }
                }
        );
    }


    @GetMapping("/product/{productId}")
    public Mono<String> getProductNameById(@PathVariable String productId) {
        return productService.getProductById(productId)
                .map(Product::getProductName)
                .defaultIfEmpty("Product not found for ID: " + productId);
    }


    @PostMapping(value = "/products", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<Void> uploadProducts(@RequestBody String csvData) {
        log.info("Received products CSV data for processing");

        if (csvData == null || csvData.trim().isEmpty()) {
            log.error("Received empty CSV data");
            return Mono.empty();
        }

        return Mono.just(csvData)
                .flatMap(data -> Flux.using(
                                () -> new StringReader(data),
                                csvParser::parseProducts,
                                reader -> {
                                    try {
                                        reader.close();
                                    } catch (Exception e) {
                                        log.error("Error closing reader", e);
                                    }
                                }
                        )
                        .collectList()
                        .flatMap(products -> {
                            log.info("Parsed {} products, loading to service", products.size());
                            return productService.loadProducts(Flux.fromIterable(products));
                        }))
                .onErrorResume(e -> {
                    log.error("Error processing products: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("Successfully completed products upload"))
                .doOnError(e -> log.error("Failed to upload products: {}", e.getMessage()));
    }
}
