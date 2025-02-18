package org.example.testtask.service;

import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.model.Trade;
import org.example.testtask.domain.service.TradeService;
import org.example.testtask.infrastructure.parser.CsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;


@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private CsvParser csvParser;

    @InjectMocks
    private TradeService tradeService;

    private Trade validTrade;
    private Product validProduct;

    @BeforeEach
    void setUp() {
        validTrade = Trade.builder()
                .productName("test")
                .date(LocalDate.now())
                .productId("1")
                .currency("USD")
                .price(new BigDecimal("100.00"))
                .build();

        validProduct = new Product("1", "test");
    }

    @Test
    void shouldLoadProducts() {
        Flux<Product> products = Flux.just(validProduct);

        Mono<Map<String, Product>> result = tradeService.loadProducts(products);

        StepVerifier.create(result)
                .expectNextMatches(map ->
                        map.containsKey("1") &&
                                map.get("1").getProductName().equals("test"))
                .verifyComplete();
    }

    @Test
    void shouldEnrichValidTrade() {
        Flux<Product> products = Flux.just(validProduct);
        tradeService.loadProducts(products).block();

        Mono<Trade> result = tradeService.enrichTradeWithProduct(validTrade);

        StepVerifier.create(result)
                .expectNextMatches(trade ->
                        trade.getProductName().equals("test") &&
                                trade.getProductId().equals("1"))
                .verifyComplete();
    }

    @Test
    void shouldHandleProductNotFound() {
        Trade tradeWithInvalidProduct = Trade.builder()
                .productName("")
                .date(LocalDate.now())
                .productId("999")
                .currency("USD")
                .price(new BigDecimal("100.00"))
                .build();

        Mono<Trade> result = tradeService.enrichTradeWithProduct(tradeWithInvalidProduct);

        StepVerifier.create(result)
                .expectNextMatches(trade ->
                        trade.getProductName().equals("Missing Product Name") &&
                                trade.getProductId().equals("999"))
                .verifyComplete();
    }

    @Test
    void shouldEnrichMultipleTrades() {
        Flux<Product> products = Flux.just(validProduct);
        tradeService.loadProducts(products).block();

        Flux<Trade> trades = Flux.just(validTrade);


        Mono<Void> result = tradeService.enrichTrades(trades);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyProductCache() {
        Mono<Trade> result = tradeService.enrichTradeWithProduct(validTrade);

        StepVerifier.create(result)
                .expectNextMatches(trade ->
                        trade.getProductName().equals("Missing Product Name") &&
                                trade.getProductId().equals("1"))
                .verifyComplete();
    }
}