package org.example.testtask.infrastructure.parser;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.example.testtask.domain.model.Product;
import org.example.testtask.domain.model.Trade;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
@Component
public class CsvParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd


    public Flux<Trade> parseTrades(Reader reader) {
        return Flux.using(
                () -> new CSVReader(reader),
                csvReader -> Flux.create(emitter -> {
                    try {
                        // Пропускаємо заголовок
                        csvReader.skip(1);
                        String[] row;
                        while ((row = csvReader.readNext()) != null) {
                            try {
                                Trade trade = createTradeFromRow(row);
                                emitter.next(trade);
                            } catch (Exception e) {
                                log.error("Error parsing trade row: {}. Skipping row.", String.join(",", row), e);
                            }
                        }
                        emitter.complete();
                    } catch (IOException | CsvValidationException ex) {
                        emitter.error(ex);
                    }
                }),
                csvReader -> {
                    try {
                        csvReader.close();
                    } catch (IOException e) {
                        log.error("Error closing CSVReader", e);
                    }
                }
        );
    }


    public Flux<Product> parseProducts(Reader reader) {
        return Flux.using(
                () -> new BufferedReader(reader),
                bufferedReader -> Flux.fromStream(bufferedReader.lines())
                        .skip(1)
                        .map(this::createProductFromLine)
                        .filter(Objects::nonNull)
                        .onErrorContinue((error, line) ->
                                log.error("Error parsing product line: {}. Skipping line.", line, error)),
                bufferedReader -> {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        log.error("Error closing BufferedReader", e);
                    }
                }
        );
    }


    public Product createProductFromLine(String line) {
        String[] fields = line.split(",");
        if (fields.length < 2 || fields[0].trim().isEmpty() || fields[1].trim().isEmpty()) {
            log.error("Invalid product line: {}", line);
            return null; // Return null if the line is invalid
        }
        return new Product(fields[0].trim(), fields[1].trim());
    }


    private Trade createTradeFromRow(String[] row) {
        try {
            LocalDate date = LocalDate.parse(row[0].trim(), DATE_FORMATTER);
            BigDecimal price = new BigDecimal(row[3].trim());
            return Trade.builder()
                    .date(date)
                    .productId(row[1].trim())
                    .currency(row[2].trim())
                    .price(price)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse trade row: " + String.join(",", row), e);
        }
    }
}
