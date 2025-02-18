package org.example.testtask.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
    private LocalDate date;
    private String productId;
    private String productName;
    private String currency;
    private BigDecimal price;

    @Builder(builderMethodName = "partialBuilder")
    public Trade(LocalDate date, String productId, String currency, BigDecimal price) {
        this.date = date;
        this.productId = productId;
        this.currency = currency;
        this.price = price;
    }

    public static Trade fromCsv(String date, String productId, String currency, String price) {

        return Trade.builder()
                .date(LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE))
                .productId(productId)
                .currency(currency)
                .price(new BigDecimal(price))
                .build();
    }

    public Trade withProductName(String newProductName) {
        return Trade.builder()
                .date(this.date)
                .productId(this.productId)
                .productName(newProductName)
                .currency(this.currency)
                .price(this.price)
                .build();
    }
}
