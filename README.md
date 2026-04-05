# ⚡ TradeFlow — Trade Processing Platform

A full-stack trade processing system built with **Apache Kafka**, **Apache Cassandra**, **Spring Boot**, and **React**. Designed to mirror the real-time event-driven architecture used at JPMorgan Chase for trade ingestion, validation, and persistence.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────┐
│  React UI (localhost:3000)                      │
│  Submit trades · Search by symbol/trader        │
│  Live stats · Status badges                     │
└──────────────────┬──────────────────────────────┘
                   │ HTTP POST /api/trades
                   │ HTTP GET  /api/trades/symbol/{symbol}
                   │ HTTP GET  /api/trades/trader/{traderId}
                   │ HTTP GET  /api/trades/{tradeId}
┌──────────────────▼──────────────────────────────┐
│  Spring Boot API (localhost:8080)               │
│  TradeController · TradeEventProducer           │
│  TradeQueryService · CORS enabled               │
└──────────┬──────────────────────────────────────┘
           │ Kafka publish (symbol as partition key)
┌──────────▼──────────────────────────────────────┐
│  Apache Kafka (localhost:9092)                  │
│  Topic: trade-events · 3 partitions             │
│  Symbol-keyed → guaranteed order per instrument │
└──────────┬──────────────────────────────────────┘
           │ @KafkaListener · manual acknowledgment
┌──────────▼──────────────────────────────────────┐
│  Trade Consumer + Validator                     │
│  EXECUTED / FAILED · offset commit on success   │
│  TradePersistenceService                        │
└──────────┬──────────────────────────────────────┘
           │ Writes to 3 tables simultaneously
┌──────────▼──────────────────────────────────────┐
│  Apache Cassandra (localhost:9042)              │
│  trades_by_symbol  ← query by instrument        │
│  trades_by_trader  ← query by desk/trader       │
│  trades_by_id      ← lookup by trade ID         │
└─────────────────────────────────────────────────┘
```

---

## 🧰 Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Backend | Spring Boot | 3.x |
| Language | Java | 21 |
| Build | Maven | 3.x |
| Messaging | Apache Kafka | 7.6.0 (Confluent) |
| Database | Apache Cassandra | 4.1 |
| Frontend | React | 18.x |
| HTTP Client | Axios | latest |
| Containers | Docker + Docker Compose | latest |

---

## 📋 Prerequisites

- Java 21
- Maven 3.x
- Docker Desktop
- Node.js 18+ and npm
- IntelliJ IDEA (recommended)

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd tradeflow
```

### 2. Start Infrastructure (Kafka + Cassandra)

```bash
docker compose up -d
```

Verify all containers are running:

```bash
docker compose ps
```

Expected output:

```
NAME                  STATUS
tradeflow-zookeeper   running
tradeflow-kafka       running
tradeflow-cassandra   running
```

> ⚠️ Cassandra takes 30–60 seconds to fully initialize. Wait before proceeding.

### 3. Create the Kafka Topic

```bash
docker exec tradeflow-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic trade-events \
  --partitions 3 \
  --replication-factor 1
```

Verify the topic:

```bash
docker exec tradeflow-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic trade-events
```

### 4. Create the Cassandra Schema

```bash
docker exec -it tradeflow-cassandra cqlsh
```

Run the following inside `cqlsh`:

```sql
CREATE KEYSPACE IF NOT EXISTS tradeflow
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE tradeflow;

-- Query by symbol (e.g. all AAPL trades)
CREATE TABLE IF NOT EXISTS trades_by_symbol (
    symbol      TEXT,
    timestamp   TIMESTAMP,
    trade_id    UUID,
    trader_id   TEXT,
    side        TEXT,
    quantity    INT,
    price       DECIMAL,
    status      TEXT,
    exchange    TEXT,
    PRIMARY KEY ((symbol), timestamp, trade_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, trade_id ASC);

-- Query by trader (e.g. all trades by trader-001)
CREATE TABLE IF NOT EXISTS trades_by_trader (
    trader_id   TEXT,
    timestamp   TIMESTAMP,
    trade_id    UUID,
    symbol      TEXT,
    side        TEXT,
    quantity    INT,
    price       DECIMAL,
    status      TEXT,
    exchange    TEXT,
    PRIMARY KEY ((trader_id), timestamp, trade_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, trade_id ASC);

-- Lookup by trade ID
CREATE TABLE IF NOT EXISTS trades_by_id (
    trade_id    UUID PRIMARY KEY,
    trader_id   TEXT,
    symbol      TEXT,
    side        TEXT,
    quantity    INT,
    price       DECIMAL,
    status      TEXT,
    exchange    TEXT,
    timestamp   TIMESTAMP
);
```

Type `exit` to leave cqlsh.

### 5. Start the Spring Boot Backend

```bash
./mvnw spring-boot:run
```

Or run `TradeflowApplication.java` directly from IntelliJ IDEA.

The API will be available at `http://localhost:8080`.

### 6. Start the React Frontend

```bash
cd tradeflow-ui
npm install
npm start
```

The UI will open automatically at `http://localhost:3000`.

---

## 🐳 Docker Compose Reference

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    ports: ["2181:2181"]

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  cassandra:
    image: cassandra:4.1
    ports: ["9042:9042"]
    volumes: [cassandra_data:/var/lib/cassandra]
```

Stop all containers:

```bash
docker compose down
```

Stop and remove all data volumes:

```bash
docker compose down -v
```

---

## 📡 API Reference

### Submit a Trade

```
POST /api/trades
Content-Type: application/json
```

Request body:

```json
{
  "traderId": "trader-001",
  "symbol": "AAPL",
  "side": "BUY",
  "quantity": 1000,
  "price": 189.50,
  "exchange": "NASDAQ"
}
```

Response `202 Accepted`:

```json
{
  "tradeId": "4b87a1b7-1ee7-47b0-b9dd-ca198f66cf23",
  "traderId": "trader-001",
  "symbol": "AAPL",
  "side": "BUY",
  "quantity": 1000,
  "price": 189.50,
  "status": "PENDING",
  "timestamp": "2026-04-05T09:44:10.381Z",
  "exchange": "NASDAQ"
}
```

### Get Trade by ID

```
GET /api/trades/{tradeId}
```

### Get Trades by Symbol

```
GET /api/trades/symbol/{symbol}
```

Example: `GET /api/trades/symbol/AAPL`

### Get Trades by Trader

```
GET /api/trades/trader/{traderId}
```

Example: `GET /api/trades/trader/trader-001`

### Valid Values

| Field | Valid Values |
|---|---|
| `side` | `BUY`, `SELL` |
| `exchange` | `NYSE`, `NASDAQ`, `LSE`, `CME` |
| `quantity` | Positive integer |
| `price` | Positive decimal |
| Max trade value | $50,000,000 |

---

## 🔄 Trade Lifecycle

```
POST /api/trades
      │
      ▼
TradeEventProducer
  - Assigns tradeId (UUID)
  - Sets status = PENDING
  - Sets timestamp
  - Publishes to Kafka (keyed by symbol)
      │
      ▼
Kafka topic: trade-events (3 partitions)
  - Same symbol → always same partition
  - Guarantees ordering per instrument
      │
      ▼
TradeEventConsumer (@KafkaListener)
  - Receives message
  - Validates (side, exchange, quantity, price, $50M limit)
      │
      ├── Valid   → status = EXECUTED
      └── Invalid → status = FAILED
      │
      ▼
TradePersistenceService
  - Writes to trades_by_symbol
  - Writes to trades_by_trader
  - Writes to trades_by_id
      │
      ▼
ack.acknowledge()
  - Offset committed to Kafka
  - Message removed from queue
```

---

## 🗄️ Cassandra Data Model

### Design Philosophy — Query-Driven Modeling

Cassandra requires tables to be designed around specific query patterns, not around the data itself. Every query needs its own optimized table. This project implements three access patterns with three dedicated tables.

### Table 1: `trades_by_symbol`

**Query pattern:** All trades for a given instrument, newest first.

```sql
SELECT * FROM trades_by_symbol WHERE symbol = 'AAPL';
```

**Primary key:** `((symbol), timestamp DESC, trade_id ASC)`
- Partition key: `symbol` — all AAPL trades on the same node
- Clustering: `timestamp DESC` — newest trades returned first automatically

### Table 2: `trades_by_trader`

**Query pattern:** All trades submitted by a specific trader, newest first.

```sql
SELECT * FROM trades_by_trader WHERE trader_id = 'trader-001';
```

**Primary key:** `((trader_id), timestamp DESC, trade_id ASC)`

### Table 3: `trades_by_id`

**Query pattern:** Look up a specific trade by its unique ID.

```sql
SELECT * FROM trades_by_id WHERE trade_id = 682892b9-7b56-47ac-a4e1-b26da73df071;
```

**Primary key:** `trade_id` (simple partition key)

### Why Three Tables?

Every trade is written to all three tables simultaneously. Storage is cheap. Query performance at scale is everything. This is the standard Cassandra pattern used at companies processing billions of records daily.

---

## 📨 Kafka Configuration

### Topic: `trade-events`

| Property | Value | Reason |
|---|---|---|
| Partitions | 3 | 3 parallel consumer threads |
| Replication factor | 1 | Single broker (dev only — use 3 in prod) |
| Partition key | `symbol` | Guarantees ordering per instrument |

### Producer Settings

| Setting | Value | Reason |
|---|---|---|
| `enable.idempotence` | `true` | Exactly-once delivery guarantee |
| `acks` | `-1` (all) | Wait for all in-sync replicas |
| `key-serializer` | `StringSerializer` | Symbol used as string key |
| `value-serializer` | `JsonSerializer` | Trade events as JSON |

### Consumer Settings

| Setting | Value | Reason |
|---|---|---|
| `auto-offset-reset` | `earliest` | Read from beginning on fresh start |
| `enable-auto-commit` | `false` | Manual acknowledgment only |
| `AckMode` | `MANUAL_IMMEDIATE` | Commit only after successful processing |
| `concurrency` | `3` | One thread per partition |

### The Manual Acknowledgment Pattern

```
Receive message
      │
      ▼
Process (validate + persist)
      │
      ├── Success → ack.acknowledge() → offset committed
      └── Exception → no ack → message redelivered after visibility timeout
```

This guarantees at-least-once processing. No trade event is ever lost.

---

## 📁 Project Structure

```
tradeflow/
├── docker-compose.yml
├── pom.xml
└── src/main/java/com/tradeflow/
    ├── TradeflowApplication.java
    ├── config/
    │   ├── AwsConfig.java
    │   └── KafkaConfig.java
    ├── controller/
    │   └── TradeController.java
    ├── model/
    │   ├── TradeEvent.java          ← Kafka message model
    │   ├── TradeResponse.java       ← API response DTO
    │   ├── TradeBySymbol.java       ← Cassandra entity
    │   ├── TradeByTrader.java       ← Cassandra entity
    │   └── TradeById.java           ← Cassandra entity
    ├── producer/
    │   └── TradeEventProducer.java
    ├── consumer/
    │   ├── TradeEventConsumer.java
    │   └── TradeValidator.java
    ├── repository/
    │   ├── TradeBySymbolRepository.java
    │   ├── TradeByTraderRepository.java
    │   └── TradeByIdRepository.java
    └── service/
        ├── TradePersistenceService.java
        └── TradeQueryService.java

tradeflow-ui/
├── package.json
└── src/
    └── App.js                       ← Single-file React dashboard
```

---

## 🧪 Testing the Full Pipeline

### 1. Submit trades via curl

```bash
# Valid BUY trade
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "traderId": "trader-001",
    "symbol": "AAPL",
    "side": "BUY",
    "quantity": 1000,
    "price": 189.50,
    "exchange": "NASDAQ"
  }'

# Valid SELL trade
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "traderId": "trader-002",
    "symbol": "JPM",
    "side": "SELL",
    "quantity": 500,
    "price": 198.75,
    "exchange": "NYSE"
  }'

# Invalid trade (bad exchange — will FAIL validation)
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "traderId": "trader-003",
    "symbol": "MSFT",
    "side": "BUY",
    "quantity": 200,
    "price": 415.00,
    "exchange": "INVALID"
  }'
```

### 2. Query trades

```bash
# By symbol
curl http://localhost:8080/api/trades/symbol/AAPL

# By trader
curl http://localhost:8080/api/trades/trader/trader-001

# By trade ID
curl http://localhost:8080/api/trades/{tradeId}
```

### 3. Verify directly in Cassandra

```bash
docker exec -it tradeflow-cassandra cqlsh -e \
  "SELECT trade_id, symbol, side, quantity, price, status FROM tradeflow.trades_by_symbol WHERE symbol='AAPL';"
```

### 4. Monitor Kafka topic

```bash
docker exec tradeflow-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic trade-events \
  --from-beginning
```

---

## 💡 Key Concepts & Interview Talking Points

### Kafka Partition Key Strategy

All trade events are keyed by `symbol`. Kafka hashes the key to determine the target partition. This means:

- All `AAPL` trades always land on the same partition
- All `JPM` trades always land on the same partition
- Ordering is guaranteed **per symbol**, not globally
- Up to 3 trades for different symbols can be processed **simultaneously** on different threads

### CQRS — Command Query Responsibility Segregation

The write path and read path are completely separate:

- **Write (Command):** `POST /api/trades` → Kafka → Consumer → Cassandra
- **Read (Query):** `GET /api/trades/**` → Cassandra directly

The API returns `202 Accepted` immediately on write. Processing is asynchronous. The UI refreshes after a short delay to show the processed result.

### Cassandra vs PostgreSQL for Trade Data

| Requirement | PostgreSQL | Cassandra |
|---|---|---|
| Massive write throughput | Struggles | Native strength |
| Time-series queries | Needs careful indexing | Designed for it |
| Horizontal scale | Complex | Add nodes linearly |
| Single point of failure | Needs Multi-AZ | Masterless by design |
| Query flexibility | Any column | Must design per query |

### Manual Kafka Acknowledgment

The consumer commits the offset **only after** successful validation and persistence. If the service crashes or Cassandra is unavailable, the message is redelivered automatically. No trade is ever silently lost.

### Trade Validation Rules

- Symbol: required, non-blank
- Side: must be `BUY` or `SELL`
- Quantity: must be positive integer
- Price: must be positive decimal
- Exchange: must be `NYSE`, `NASDAQ`, `LSE`, or `CME`
- Trade value: must not exceed $50,000,000

---

## 🔧 Useful Docker Commands

```bash
# View container logs
docker logs tradeflow-kafka -f
docker logs tradeflow-cassandra -f

# List Kafka topics
docker exec tradeflow-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Describe consumer group
docker exec tradeflow-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group tradeflow-consumer-group

# Open Cassandra shell
docker exec -it tradeflow-cassandra cqlsh

# Restart a single service
docker compose restart kafka

# Full reset (removes all data)
docker compose down -v && docker compose up -d
```

---

## 🚧 Known Limitations (Dev Environment)

- `replication-factor: 1` — single Kafka broker, no redundancy. Use 3 in production.
- Cassandra `SimpleStrategy` replication — use `NetworkTopologyStrategy` in production.
- No authentication on Kafka or Cassandra — add SASL/SSL and Cassandra auth for production.
- React UI polls on a fixed delay after submit — replace with WebSocket for true real-time updates.
- No dead letter topic configured for Kafka — add one to capture permanently failed messages.

---

## 🗺️ Suggested Next Steps

| Enhancement | Concepts Covered |
|---|---|
| Add Kafka Dead Letter Topic | Mirror SQS DLQ pattern in Kafka |
| WebSocket push to React | Real-time trade feed without polling |
| Dockerize Spring Boot app | Add app container to docker-compose.yml |
| Trade position aggregator | Stateful streaming, net position per symbol |
| Spring Security + JWT | API authentication, JPMC API Gateway pattern |
| Deploy to AWS ECS + RDS | Connect TradeFlow to LoanFlow AWS infrastructure |
| Kafka Streams | Real-time aggregations — total volume per symbol |

---

## 📚 References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Cassandra Documentation](https://cassandra.apache.org/doc/latest/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Spring Data Cassandra](https://docs.spring.io/spring-data/cassandra/docs/current/reference/html/)
- [Confluent Platform Docker Images](https://hub.docker.com/u/confluentinc)

---

*Built as part of JPMC Senior Java Engineer interview preparation — April 2026*
