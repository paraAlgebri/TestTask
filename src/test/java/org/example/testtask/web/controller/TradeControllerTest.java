package org.example.testtask.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.model.Trade;
import org.example.testtask.domain.service.ProductService;
import org.example.testtask.domain.service.TradeService;
import org.example.testtask.infrastructure.parser.CsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@WebFluxTest(TradeController.class)
@ContextConfiguration(classes = TradeControllerTest.TestConfig.class)
public class TradeControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        CsvParser csvParser() {
            return mock(CsvParser.class);
        }

        @Bean
        TradeService tradeService() {
            return mock(TradeService.class);
        }

        @Bean
        ProductService productService() {
            return mock(ProductService.class);
        }

        @Bean
        TradeController tradeController(CsvParser csvParser, TradeService tradeService, ProductService productService) {
            return new TradeController(csvParser, tradeService, productService);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CsvParser csvParser;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private ProductService productService;

    private static final String VALID_CSV_HEADER = "date,productId,currency,price";

    @BeforeEach
    void setUp() {
        log.info("Starting test setup - resetting all mocks");
        reset(csvParser, tradeService, productService);
        log.info("Test setup completed");
    }

    @Test
    @DisplayName("Should correctly process CSV trade file")
    void processTradeFile() throws IOException {
        log.info("Starting processTradeFile test");

        String tradeCsvContent = new String(
                new ClassPathResource("trade.csv").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        log.info("Loaded trade.csv content:\n{}", tradeCsvContent);

        List<Trade> expectedTrades = List.of(
                new Trade(LocalDate.of(2023, 1, 1), "1", "USD", new BigDecimal("100.25")),
                new Trade(LocalDate.of(2023, 1, 1), "2", "EUR", new BigDecimal("200.45")),
                new Trade(LocalDate.of(2023, 1, 1), "3", "GBP", new BigDecimal("300.50")),
                new Trade(LocalDate.of(2023, 1, 2), "4", "USD", new BigDecimal("150.75")),
                new Trade(LocalDate.of(2023, 1, 2), "5", "EUR", new BigDecimal("250.85")),
                new Trade(LocalDate.of(2023, 1, 3), "6", "GBP", new BigDecimal("350.95")),
                new Trade(LocalDate.of(2023, 1, 3), "7", "USD", new BigDecimal("400.10")),
                new Trade(LocalDate.of(2023, 1, 4), "8", "EUR", new BigDecimal("450.20")),
                new Trade(LocalDate.of(2023, 1, 4), "9", "GBP", new BigDecimal("500.30")),
                new Trade(LocalDate.of(2023, 1, 5), "10", "USD", new BigDecimal("550.40")),
                new Trade(LocalDate.of(2023, 1, 5), "11", "EUR", new BigDecimal("600.50")),
                new Trade(LocalDate.of(2023, 1, 6), "2", "USD", new BigDecimal("700.60")),
                new Trade(LocalDate.of(2023, 1, 6), "3", "EUR", new BigDecimal("800.70")),
                new Trade(LocalDate.of(2023, 1, 7), "12", "GBP", new BigDecimal("900.80")),
                new Trade(LocalDate.of(2023, 1, 7), "13", "USD", new BigDecimal("1000.90")),
                new Trade(LocalDate.of(2023, 1, 8), "1", "EUR", new BigDecimal("1100.10")),
                new Trade(LocalDate.of(2023, 1, 8), "4", "GBP", new BigDecimal("1200.20")),
                new Trade(LocalDate.of(2023, 1, 9), "5", "USD", new BigDecimal("1300.30")),
                new Trade(LocalDate.of(2023, 1, 9), "6", "EUR", new BigDecimal("1400.40")),
                new Trade(LocalDate.of(2023, 1, 10), "7", "GBP", new BigDecimal("1500.50")),
                new Trade(LocalDate.of(2013, 12, 31), "10", "USD", new BigDecimal("1600.60")),
                new Trade(LocalDate.of(2023, 1, 11), "5", "GBP", new BigDecimal("1800.80"))
        );
        log.info("Created {} expected trade objects", expectedTrades.size());

        Map<String, Product> productMap = expectedTrades.stream()
                .collect(Collectors.toMap(
                        Trade::getProductId,
                        trade -> new Product(trade.getProductId(), "Product " + trade.getProductId()),
                        (existing, replacement) -> existing
                ));
        log.info("Created product map with {} unique products", productMap.size());

        when(csvParser.parseTrades(any(Reader.class)))
                .thenReturn(Flux.fromIterable(expectedTrades));
        log.info("Configured csvParser mock to return {} trades", expectedTrades.size());

        when(tradeService.enrichTradeWithProduct(any(Trade.class)))
                .thenAnswer(invocation -> {
                    Trade trade = invocation.getArgument(0);
                    Product product = productMap.get(trade.getProductId());
                    log.debug("Enriching trade with productId: {} [date: {}, currency: {}]",
                            trade.getProductId(), trade.getDate(), trade.getCurrency());
                    if (product != null) {
                        return Mono.just(trade.withProductName(product.getProductName()));
                    }
                    return Mono.just(trade.withProductName("Missing Product Name"));
                });
        log.info("Configured tradeService mock with product enrichment logic");

        log.info("Sending POST request to /api/v1/enrich endpoint");
        List<Trade> actualTrades = webTestClient.post()
                .uri("/api/v1/enrich")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(tradeCsvContent)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Trade.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(actualTrades, "Response body should not be null");
        log.info("Received {} trades in response", actualTrades.size());

        log.info("Verifying received trades:");
        actualTrades.forEach(trade ->
                log.info("Trade - Date: {}, ProductId: {}, Currency: {}, Price: {}, ProductName: {}",
                        trade.getDate(), trade.getProductId(), trade.getCurrency(),
                        trade.getPrice(), trade.getProductName())
        );

        assertEquals(expectedTrades.size(), actualTrades.size(),
                String.format("Expected %d trades but got %d", expectedTrades.size(), actualTrades.size()));
        log.info("Trade count verification passed");

        log.info("Starting StepVerifier verification");
        StepVerifier.create(Flux.fromIterable(actualTrades))
                .recordWith(ArrayList::new)
                .thenConsumeWhile(trade -> true)
                .consumeRecordedWith(trades -> {
                    assertEquals(expectedTrades.size(), trades.size());
                    trades.forEach(trade -> {
                        assertNotNull(trade.getProductName(),
                                "Product name should not be null for trade: " + trade.getProductId());
                        log.debug("Verified trade: {}", trade);
                    });
                })
                .verifyComplete();
        log.info("StepVerifier verification completed successfully");

        verify(csvParser).parseTrades(any(Reader.class));
        verify(tradeService, times(expectedTrades.size())).enrichTradeWithProduct(any(Trade.class));
        log.info("Mock verifications completed successfully");
    }


    @Test
    @DisplayName("Should process products.csv file")
    void processProductFile() throws IOException {
        log.info("Starting processProductFile test");

        byte[] fileBytes = new ClassPathResource("largeSizeProduct.csv").getInputStream().readAllBytes();
        String productsCsvContent = new String(fileBytes, StandardCharsets.UTF_8);

        log.info("Test CSV content:\n{}", productsCsvContent);

        List<Product> expectedProducts = Arrays.asList(
                new Product("1", "Commodity Swaps 1"),
                new Product("2", "Commodity Swaps"),
                new Product("3", "FX Forward"),
                new Product("4", "Government Bonds Domestic"),
                new Product("5", "Convertible Bonds Domestic")
        );

        when(csvParser.parseProducts(any(Reader.class)))
                .thenReturn(Flux.fromIterable(expectedProducts));

        when(productService.loadProducts(any(Flux.class)))
                .thenReturn(Mono.empty());

        log.info("Sending POST request to /api/v1/products");
        webTestClient.post()
                .uri("/api/v1/products")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(productsCsvContent)
                .exchange()
                .expectStatus().isOk();

        verify(csvParser).parseProducts(any(Reader.class));
        verify(productService).loadProducts(any(Flux.class));

        log.info("Products processing test completed successfully");
    }

    @Test
    @DisplayName("Should handle empty CSV file")
    void enrichTradeData_EmptyFile_Success() {
        log.info("Starting empty CSV file test");

        String csvContent = VALID_CSV_HEADER + "\n";
        log.info("Testing with empty CSV content (header only): {}", csvContent);

        when(csvParser.parseTrades(any())).thenReturn(Flux.empty());
        log.info("Configured csvParser to return empty Flux");

        log.info("Sending POST request to /api/v1/enrich endpoint with empty CSV");
        webTestClient.post()
                .uri("/api/v1/enrich")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(csvContent)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Trade.class)
                .hasSize(0);

        log.info("Empty CSV file test completed successfully");
        verify(csvParser).parseTrades(any());
        log.info("Verified csvParser was called once");
    }

    @Test
    @DisplayName("Should get product name by ID successfully")
    void getProductNameById_Success() {
        String productId = "1";
        log.info("Starting getProductNameById test with productId: {}", productId);

        Product product = new Product(productId, "Test Product");
        log.info("Created test product: ID={}, Name={}", product.getProductId(), product.getProductName());

        when(productService.getProductById(productId)).thenReturn(Mono.just(product));
        log.info("Configured productService mock to return test product");

        log.info("Sending GET request to /api/v1/product/{}", productId);
        webTestClient.get()
                .uri("/api/v1/product/{productId}", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Test Product");

        verify(productService).getProductById(productId);
        log.info("Product retrieval test completed successfully");
    }

    @Test
    @DisplayName("Should handle non-existent product ID")
    void getProductNameById_NotFound() {
        String productId = "999";
        log.info("Starting non-existent product test with productId: {}", productId);

        when(productService.getProductById(productId)).thenReturn(Mono.empty());
        log.info("Configured productService to return empty Mono for non-existent product");

        log.info("Sending GET request to /api/v1/product/{}", productId);
        webTestClient.get()
                .uri("/api/v1/product/{productId}", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Product not found for ID: " + productId);

        verify(productService).getProductById(productId);
        log.info("Non-existent product test completed successfully");
    }

    @Test
    @DisplayName("Should handle empty CSV data")
    void uploadProducts_EmptyData_Success() {
        log.info("Starting empty CSV data test");

        String emptyCsvData = "";

        log.info("Sending POST request to /api/v1/products with empty CSV data");
        webTestClient.post()
                .uri("/api/v1/products")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(emptyCsvData)
                .exchange()
                .expectStatus().isOk();

        // Verify that productService.loadProducts() was not called with empty data
        verify(productService, never()).loadProducts(any());
        log.info("Empty CSV data test completed successfully");
    }
}