# Trade Data Enrichment Service

A reactive Spring Boot service for enriching trade data with product information, built with performance and scalability in mind.

## Features

- Reactive processing of trade data using Spring WebFlux
- Support for large datasets (millions of trades, 10k-100k products)
- Redis caching for optimal performance
- Data validation and error handling
- CSV parsing with support for multiple formats
- Asynchronous processing with non-blocking operations

## Technical Stack

- Java/Spring Boot
- Spring WebFlux for reactive programming
- Redis for caching
- Project Reactor (Flux/Mono)
- OpenCSV for CSV parsing
- Lombok for reducing boilerplate code


### Prerequisites

- JDK 17
- Redis server
- Maven

## API Documentation

### 1. Enrich Trade Data

```http
POST /api/v1/enrich
Content-Type: text/plain
```

Accepts CSV data in the format:
```
date,productId,currency,price
20240101,P001,USD,100.00
```

Returns enriched trade data with product names.

### 2. Get Product by ID

```http
GET /api/v1/product/{productId}
```

Returns product name for the given product ID.

### 3. Get Multiple Products

```http
POST /api/v1/products
Content-Type: application/json
```

Accepts an array of product IDs and returns corresponding product details.

## Data Validation

- Date format validation (yyyyMMdd)
- Product existence validation
- Missing product handling with "Missing Product Name" fallback

## Performance Features

1. Two-level Caching:
   - Local cache (ConcurrentHashMap)
   - Redis distributed cache

2. Reactive Processing:
   - Non-blocking I/O operations
   - Streaming data processing
   - Backpressure handling

3. Bulk Operations:
   - Batch processing support
   - Optimized Redis operations

## Limitations

1. CSV Format:
   - Strict format requirements
   - No support for malformed CSV data

2. Memory Usage:
   - Local cache size is unbounded
   - Large datasets might require cache eviction strategy

3. Redis Dependencies:
   - Requires running Redis instance
   - Network latency impact on performance

## Future Improvements

1. Enhanced Format Support:
   - Add JSON and XML support
   - Implement format auto-detection

2. Cache Optimization:
   - Implement cache eviction policies

3. Performance Enhancements:
   - Implement batch processing for trades
   - Add parallel processing capabilities

4. Error Handling:
   - Add retry mechanisms for failed operations


## Error Handling

The service implements  error handling:

1. Data Validation Errors:
   - Invalid date format logging
   - Missing product mapping logging

2. Processing Errors:
   - Redis connection issues
   - CSV parsing errors
   - Invalid data format

3. Cache Errors:
   - Cache miss handling
   - Redis connection failure fallback


