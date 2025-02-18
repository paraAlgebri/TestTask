package org.example.testtask.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TradeTest {
    @Test
    void shouldCreateTradeFromCsv() {
        String date = "20250101";
        String productId = "1";
        String currency = "USD";
        String price = "100.25";

        Trade trade = Trade.fromCsv(date, productId, currency, price);

        assertNotNull(trade);
        assertEquals(LocalDate.of(2025, 1, 1), trade.getDate());
        assertEquals(productId, trade.getProductId());
        assertEquals(currency, trade.getCurrency());
        assertEquals(new BigDecimal("100.25"), trade.getPrice());
    }

    @Test
    void shouldCreateTradeWithProductName() {
        Trade trade = Trade.builder()
                .date(LocalDate.now())
                .productId("1")
                .currency("USD")
                .price(new BigDecimal("100.00"))
                .build();

        Trade enrichedTrade = trade.withProductName("Test Product");

        assertEquals("Test Product", enrichedTrade.getProductName());
        assertEquals(trade.getDate(), enrichedTrade.getDate());
        assertEquals(trade.getProductId(), enrichedTrade.getProductId());
        assertEquals(trade.getCurrency(), enrichedTrade.getCurrency());
        assertEquals(trade.getPrice(), enrichedTrade.getPrice());
    }
}
