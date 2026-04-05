# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start infrastructure (Kafka, Zookeeper, Cassandra)
docker-compose up -d

# Build
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=TradeflowApplicationTests

# Run application
./mvnw spring-boot:run

# Build Docker image
./mvnw spring-boot:build-image
```

## Architecture

TradeFlow is an event-driven financial trading system built with **Spring Boot 4** and **Apache Kafka**. The current implementation covers the ingest path only — a consumer and Cassandra persistence layer are not yet implemented (Cassandra auto-configuration is explicitly disabled in `TradeflowApplication.java`).

### Request Flow

```
POST /api/trades
  → TradeController (assigns 202 Accepted)
  → TradeEventProducer (assigns tradeId UUID, timestamp, PENDING status)
  → Kafka topic: trade-events (partitioned by symbol for per-symbol ordering)
```

### Key Design Decisions

- **Symbol as Kafka partition key** — ensures trade events for the same stock symbol are ordered within a partition.
- **202 Accepted response** — all trade submissions are async; the HTTP response does not reflect execution outcome.
- **Type headers disabled** — Kafka JSON serialization uses `spring.json.add.type.headers=false` to keep messages schema-agnostic.

### Infrastructure (docker-compose)

| Service | Port | Notes |
|---|---|---|
| Kafka | 9092 | Topic `trade-events`, auto-create enabled |
| Zookeeper | 2181 | Coordination for Kafka |
| Cassandra | 9042 | Keyspace `tradeflow`, cluster `TradeFlowCluster` — not yet wired in |

### Package Structure

- `com.tradeflow.model` — `TradeEvent` POJO (symbol, side BUY/SELL, quantity, price, exchange NYSE/NASDAQ/LSE, status PENDING/EXECUTED/FAILED/CANCELLED)
- `com.tradeflow.controller` — REST API (`POST /api/trades`)
- `com.tradeflow.producer` — Kafka publisher

### What's Missing / In Progress

- Kafka consumer service
- Cassandra repository and persistence logic (dependency is present but auto-config is excluded)
- Tests beyond the basic context load test
