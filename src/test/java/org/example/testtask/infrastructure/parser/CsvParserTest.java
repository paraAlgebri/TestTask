package org.example.testtask.infrastructure.parser;

import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

class CsvParserTest {

    private CsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvParser();
    }

    @Test
    void testParseTradesValidData() {
        String csvContent = "date,productId,currency,price\n" +
                "20230101,1,USD,100.25\n" +
                "20230102,4,USD,150.75\n";

        Reader reader = new StringReader(csvContent);

        Flux<Trade> tradeFlux = csvParser.parseTrades(reader);

        StepVerifier.create(tradeFlux)
                .expectNextMatches(trade -> trade.getDate().equals(LocalDate.parse("20230101", DateTimeFormatter.BASIC_ISO_DATE)) &&
                        "1".equals(trade.getProductId()) &&
                        "USD".equals(trade.getCurrency()) &&
                        BigDecimal.valueOf(100.25).equals(trade.getPrice()))
                .expectNextMatches(trade -> trade.getDate().equals(LocalDate.parse("20230102", DateTimeFormatter.BASIC_ISO_DATE)) &&
                        "4".equals(trade.getProductId()) &&
                        "USD".equals(trade.getCurrency()) &&
                        BigDecimal.valueOf(150.75).equals(trade.getPrice()))
                .verifyComplete();
    }

    @Test
    void testParseTradesInvalidDateFormat() {
        String csvContent = "date,productId,currency,price\n" +
                "invalidDate,1,EUR,1700.70\n";

        Reader reader = new StringReader(csvContent);

        Flux<Trade> tradeFlux = csvParser.parseTrades(reader);

        StepVerifier.create(tradeFlux)
                .expectNextCount(0)
                .verifyComplete();
    }


    @Test
    void testParseTradesEmptyFile() {
        String csvContent = "date,productId,currency,price\n";

        Reader reader = new StringReader(csvContent);

        Flux<Trade> tradeFlux = csvParser.parseTrades(reader);

        StepVerifier.create(tradeFlux)
                .verifyComplete();
    }

    @Test
    void testParseProductsValidData() {
        String csvContent = "productId,productName\n" +
                "1,Treasury Bills Domestic\n" +
                "2,Corporate Bonds Domestic\n";

        Reader reader = new StringReader(csvContent);

        Flux<Product> productFlux = csvParser.parseProducts(reader);

        StepVerifier.create(productFlux)
                .expectNextMatches(product -> "1".equals(product.getProductId()) &&
                        "Treasury Bills Domestic".equals(product.getProductName()))
                .expectNextMatches(product -> "2".equals(product.getProductId()) &&
                        "Corporate Bonds Domestic".equals(product.getProductName()))
                .verifyComplete();
    }


    @Test
    void testParseTradesErrorHandling() {
        String csvContent = "date,productId,currency,price\n" +
                "invalidDate,1,EUR,1700.70\n" +  // Invalid date
                "20230102,4,USD,invalidPrice\n";  // Invalid price

        Reader reader = new StringReader(csvContent);

        Flux<Trade> tradeFlux = csvParser.parseTrades(reader);

        StepVerifier.create(tradeFlux)
                .expectNextCount(0)
                .verifyComplete();
    }


    @Test
    void testParseProductsErrorHandling() {
        String csvContent = "productId,productName\n" +
                "1\n";  // Missing productName

        Reader reader = new StringReader(csvContent);

        Flux<Product> productFlux = csvParser.parseProducts(reader);

        StepVerifier.create(productFlux)
                .expectNextCount(0)
                .verifyComplete();
    }

}
