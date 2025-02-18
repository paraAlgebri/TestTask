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
import java.time.format.DateTimeFormatter;
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
        reset(csvParser, tradeService, productService);
    }

    @Test
    @DisplayName("Should correctly process CSV trade file")
    void processTradeFile() throws IOException {
        String tradeCsvContent = new String(
                new ClassPathResource("trade.csv").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        log.info("CSV content:\n{}", tradeCsvContent);

        // Expected Trade objects
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

        Map<String, Product> productMap = expectedTrades.stream()
                .collect(Collectors.toMap(
                        Trade::getProductId,
                        trade -> new Product(trade.getProductId(), "Product " + trade.getProductId()),
                        (existing, replacement) -> existing
                ));

        // Mock csvParser behavior
        when(csvParser.parseTrades(any(Reader.class)))
                .thenReturn(Flux.fromIterable(expectedTrades));

        // Pre-populate the product cache in TradeService
        when(tradeService.enrichTradeWithProduct(any(Trade.class)))
                .thenAnswer(invocation -> {
                    Trade trade = invocation.getArgument(0);
                    Product product = productMap.get(trade.getProductId());
                    if (product != null) {
                        return Mono.just(trade.withProductName(product.getProductName()));
                    }
                    return Mono.just(trade.withProductName("Missing Product Name"));
                });

        // Execute test
        List<Trade> actualTrades = webTestClient.post()
                .uri("/api/v1/enrich")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(tradeCsvContent)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Trade.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(actualTrades, "Response body is null");

        log.info("Received Trades:");
        actualTrades.forEach(trade -> log.info("{}", trade));

        assertEquals(expectedTrades.size(), actualTrades.size(), "Number of trades does not match");

        StepVerifier.create(Flux.fromIterable(actualTrades))
                .recordWith(ArrayList::new)
                .thenConsumeWhile(trade -> true)
                .consumeRecordedWith(trades -> {
                    assertEquals(expectedTrades.size(), trades.size());
                    trades.forEach(trade ->
                            assertNotNull(trade.getProductName(), "Product name should not be null")
                    );
                })
                .verifyComplete();

        verify(csvParser).parseTrades(any(Reader.class));
        verify(tradeService, times(expectedTrades.size())).enrichTradeWithProduct(any(Trade.class));
    }


    @Test
    @DisplayName("Should process products.csv file")
    void processProductFile() throws IOException {
        byte[] fileBytes = new ClassPathResource("largeSizeProduct.csv").getInputStream().readAllBytes();
        String productsCsvContent = new String(fileBytes, StandardCharsets.UTF_8);

        log.info("CSV File Size: {} bytes", fileBytes.length);

        long lineCount = productsCsvContent.lines().count();
        log.info("CSV Line Count (including header): {}", lineCount);

        List<Product> savedProducts = webTestClient.post()
                .uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productsCsvContent)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Product.class)
                .getResponseBody()
                .doOnNext(product -> log.info("Processed product: {}", product))
                .collectList()
                .block();

    }


    @Test
    @DisplayName("Should successfully process valid CSV data with multiple trades")
    void enrichTradeData_MultipleValidTrades_Success() {
        List<Trade> trades = Arrays.asList(
                Trade.builder()
                        .date(LocalDate.parse("20230216", DateTimeFormatter.BASIC_ISO_DATE))
                        .productId("1")
                        .currency("USD")
                        .price(new BigDecimal("100.50"))
                        .build(),
                Trade.builder()
                        .date(LocalDate.parse("20230217", DateTimeFormatter.BASIC_ISO_DATE))
                        .productId("2")
                        .currency("EUR")
                        .price(new BigDecimal("95.75"))
                        .build()
        );

        List<Trade> enrichedTrades = Arrays.asList(
                Trade.builder()
                        .date(trades.get(0).getDate())
                        .productId(trades.get(0).getProductId())
                        .currency(trades.get(0).getCurrency())
                        .price(trades.get(0).getPrice())
                        .productName("Product 1")
                        .build(),
                Trade.builder()
                        .date(trades.get(1).getDate())
                        .productId(trades.get(1).getProductId())
                        .currency(trades.get(1).getCurrency())
                        .price(trades.get(1).getPrice())
                        .productName("Product 2")
                        .build()
        );

    }

    @Test
    @DisplayName("Should handle empty CSV file")
    void enrichTradeData_EmptyFile_Success() {
        String csvContent = VALID_CSV_HEADER + "\n";

        when(csvParser.parseTrades(any())).thenReturn(Flux.empty());

        webTestClient.post()
                .uri("/api/v1/enrich")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(csvContent)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Trade.class)
                .hasSize(0);
    }


    @Test
    @DisplayName("Should get product name by ID successfully")
    void getProductNameById_Success() {
        String productId = "1";
        Product product = new Product(productId, "Test Product");

        when(productService.getProductById(productId)).thenReturn(Mono.just(product));

        webTestClient.get()
                .uri("/api/v1/product/{productId}", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Test Product");

        verify(productService).getProductById(productId);
    }

    @Test
    @DisplayName("Should handle non-existent product ID")
    void getProductNameById_NotFound() {
        String productId = "999";

        when(productService.getProductById(productId)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/product/{productId}", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Product not found for ID: " + productId);
    }


    @Test
    @DisplayName("Should handle empty product IDs list")
    void getProductsByIds_EmptyList_Success() {
        when(productService.getProductsByIds(any())).thenReturn(Flux.empty());

        webTestClient.post()
                .uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Arrays.asList())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .hasSize(0);
    }


}