# ⚡ TradeFlow — Trade Processing Platform

A full-stack trade processing system built with **Apache Kafka**, **Apache Cassandra**, **Spring Boot**, **GraphQL**, and **React**. Designed to mirror the real-time event-driven architecture used at XYZ for trade ingestion, validation, and persistence.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  React SPA (localhost:3000)                             │
│                                                         │
│  / — Dashboard Page                                     │
│      Submit trades · REST queries · Live stats          │
│                                                         │
│  /graphql-explorer — GraphQL Explorer Page              │
│      Apollo Client · Field selector · JSON/Table view   │
└──────────────────┬──────────────────────────────────────┘
                   │ REST    → HTTP POST/GET /api/trades/**
                   │ GraphQL → HTTP POST    /graphql
┌──────────────────▼──────────────────────────────────────┐
│  Spring Boot API (localhost:8080)                       │
│                                                         │
│  REST:    TradeController + TradeQueryService           │
│  GraphQL: TradeGraphQLController (Spring for GraphQL)   │
│  Config:  WebConfig (Global CORS)                       │
└──────────┬──────────────────────────────────────────────┘
           │ Kafka publish (symbol as partition key)
┌──────────▼──────────────────────────────────────────────┐
│  Apache Kafka (localhost:9092)                          │
│  Topic: trade-events · 3 partitions                     │
│  Symbol-keyed → guaranteed order per instrument         │
└──────────┬──────────────────────────────────────────────┘
           │ @KafkaListener · manual acknowledgment
┌──────────▼──────────────────────────────────────────────┐
│  Trade Consumer + Validator                             │
│  EXECUTED / FAILED · offset commit on success only      │
│  TradePersistenceService                                │
└──────────┬──────────────────────────────────────────────┘
           │ Writes to 3 tables simultaneously
┌──────────▼──────────────────────────────────────────────┐
│  Apache Cassandra (localhost:9042)                      │
│  trades_by_symbol  ← query by instrument                │
│  trades_by_trader  ← query by desk/trader               │
│  trades_by_id      ← lookup by trade ID                 │
└─────────────────────────────────────────────────────────┘
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
| API | REST + GraphQL (Spring for GraphQL) | latest |
| Frontend | React | 19.x |
| GraphQL Client | Apollo Client | 3.11.8 |
| Routing | React Router DOM | 7.x |
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

Available endpoints:
- REST API: `http://localhost:8080/api/trades`
- GraphQL API: `http://localhost:8080/graphql`
- GraphiQL IDE: `http://localhost:8080/graphiql`

### 6. Start the React Frontend

```bash
cd tradeflow-ui
npm install --legacy-peer-deps
npm start
```

The UI opens automatically at `http://localhost:3000`.

> ⚠️ Use `--legacy-peer-deps` due to Apollo Client 3.x peer dependency constraints with React 19.

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

## 📡 REST API Reference

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

### Get Trades by Trader

```
GET /api/trades/trader/{traderId}
```

### Valid Field Values

| Field | Valid Values |
|---|---|
| `side` | `BUY`, `SELL` |
| `exchange` | `NYSE`, `NASDAQ`, `LSE`, `CME` |
| `quantity` | Positive integer |
| `price` | Positive decimal |
| Max trade value | $50,000,000 |

---

## 🔷 GraphQL API Reference

### Endpoint

```
POST http://localhost:8080/graphql
Content-Type: application/json
```

### Browser IDE

```
http://localhost:8080/graphiql
```

### Schema

```graphql
type Trade {
    tradeId:    ID!
    traderId:   String!
    symbol:     String!
    side:       Side!
    quantity:   Int!
    price:      Float!
    tradeValue: Float!
    status:     TradeStatus!
    exchange:   String!
    timestamp:  String!
}

enum Side {
    BUY
    SELL
}

enum TradeStatus {
    PENDING
    EXECUTED
    FAILED
    CANCELLED
}

type Query {
    tradeById(tradeId: ID!):           Trade
    tradesBySymbol(symbol: String!):   [Trade!]!
    tradesByTrader(traderId: String!): [Trade!]!
}
```

### Sample Queries

**Query by symbol — request only the fields you need:**

```graphql
query {
  tradesBySymbol(symbol: "AAPL") {
    tradeId
    symbol
    side
    quantity
    price
    tradeValue
    status
  }
}
```

**Query by trader:**

```graphql
query {
  tradesByTrader(traderId: "trader-001") {
    tradeId
    symbol
    side
    status
    tradeValue
  }
}
```

**Lookup by trade ID:**

```graphql
query {
  tradeById(tradeId: "682892b9-7b56-47ac-a4e1-b26da73df071") {
    tradeId
    symbol
    side
    quantity
    price
    status
    timestamp
  }
}
```

**Minimal projection — only two fields returned:**

```graphql
query {
  tradesBySymbol(symbol: "AAPL") {
    tradeId
    status
  }
}
```

### Query via curl

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ tradesBySymbol(symbol: \"AAPL\") { tradeId symbol side status tradeValue } }"
  }'
```

### GraphQL vs REST — When to Use Each

| Concern | REST | GraphQL |
|---|---|---|
| External integrations | ✅ Universal, easy to cache | ❌ Overkill |
| Fixed response shapes | ✅ Simple and predictable | ❌ Unnecessary flexibility |
| Front-office dashboards | ❌ Over-fetches unused fields | ✅ Client picks exact fields |
| Multiple consumers (risk, compliance, settlement) | ❌ One shape fits all | ✅ Each consumer defines its own projection |
| Schema as typed contract | ❌ Implicit | ✅ Enum-enforced, self-documenting |

Both APIs share the same `TradeQueryService` — zero business logic duplication. GraphQL is a different transport layer over the same service.

---

## 🔄 Trade Lifecycle

```
POST /api/trades
      │
      ▼
TradeEventProducer
  Assigns tradeId (UUID) · Sets status=PENDING · Sets timestamp
  Publishes to Kafka keyed by symbol
      │
      ▼
Kafka topic: trade-events (3 partitions)
  Same symbol → always same partition → ordering guaranteed per instrument
      │
      ▼
TradeEventConsumer (@KafkaListener)
  Validates: side, exchange, quantity, price, $50M limit
      │
      ├── Valid   → status = EXECUTED
      └── Invalid → status = FAILED
      │
      ▼
TradePersistenceService
  Writes to trades_by_symbol
  Writes to trades_by_trader
  Writes to trades_by_id
      │
      ▼
ack.acknowledge()
  Offset committed · Message removed from Kafka
```

---

## 🗄️ Cassandra Data Model

### Design Philosophy — Query-Driven Modeling

Cassandra requires tables to be designed around specific query patterns, not the data shape. Every access pattern gets its own optimized table. The same trade is written to all three tables simultaneously — storage is cheap, query performance is everything.

### Table 1: `trades_by_symbol`

**Query:** All trades for a given instrument, newest first.

**Primary key:** `((symbol), timestamp DESC, trade_id ASC)`
- Partition key `symbol` — all AAPL trades co-located on the same node
- Clustering `timestamp DESC` — newest trades returned first automatically

### Table 2: `trades_by_trader`

**Query:** All trades by a specific trader, newest first.

**Primary key:** `((trader_id), timestamp DESC, trade_id ASC)`

### Table 3: `trades_by_id`

**Query:** Point lookup by unique trade ID.

**Primary key:** `trade_id` (simple partition key)

---

## 📨 Kafka Configuration

### Topic: `trade-events`

| Property | Value | Reason |
|---|---|---|
| Partitions | 3 | 3 parallel consumer threads |
| Replication factor | 1 | Single broker — use 3 in production |
| Partition key | `symbol` | Guarantees ordering per instrument |

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
Process (validate + persist to Cassandra)
      │
      ├── Success   → ack.acknowledge() → offset committed
      └── Exception → no ack → message redelivered after visibility timeout
```

No trade event is ever silently lost.

---

## 🖥️ React Frontend

### Pages

| Route | Page | Description |
|---|---|---|
| `/` | Dashboard | Submit trades, view results, search by symbol/trader via REST |
| `/graphql-explorer` | GraphQL Explorer | Query trades via Apollo Client with interactive field selection |

### File Structure

```
tradeflow-ui/
├── package.json
└── src/
    ├── index.js          ← ApolloProvider + BrowserRouter setup
    ├── App.js            ← React Router shell (routes only)
    ├── DashboardPage.js  ← Trade dashboard (REST + axios)
    ├── GraphQLPage.js    ← GraphQL explorer (Apollo Client)
    └── queries.js        ← Reusable gql query definitions
```

### Apollo Client Setup (`index.js`)

```jsx
import { ApolloClient, InMemoryCache, ApolloProvider } from '@apollo/client';

const client = new ApolloClient({
  uri: 'http://localhost:8080/graphql',
  cache: new InMemoryCache(),
});
```

### Routing (`App.js`)

```jsx
import { Routes, Route } from 'react-router-dom';

export default function App() {
  return (
    <Routes>
      <Route path="/"                 element={<DashboardPage />} />
      <Route path="/graphql-explorer" element={<GraphQLPage />} />
    </Routes>
  );
}
```

### useLazyQuery Pattern (`GraphQLPage.js`)

```jsx
const [fetchBySymbol, { data, loading, error }] =
  useLazyQuery(GET_TRADES_BY_SYMBOL, { fetchPolicy: 'network-only' });

// Fires only when user clicks Run
fetchBySymbol({ variables: { symbol: 'AAPL' } });
```

`fetchPolicy: 'network-only'` bypasses Apollo's cache — always fetches fresh data. Essential for a live trading dashboard.

### GraphQL Explorer Features

- **Three query tabs** — By Symbol, By Trader, By Trade ID
- **Interactive field selector** — toggle individual fields on/off, re-run to see only selected fields returned
- **Table / JSON toggle** — switch between formatted table and raw Apollo response
- **Single trade card view** — detailed breakdown for trade ID lookups
- **Query shape preview** — shows the GraphQL query that will be sent

---

## ⚙️ Global CORS Configuration

A single `WebMvcConfigurer` bean covers all endpoints — both REST and GraphQL. This is required because `@CrossOrigin` on individual controllers does not apply to Spring for GraphQL's internal servlet mapping.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

---

## 📁 Backend Project Structure

```
tradeflow/
├── docker-compose.yml
├── pom.xml
└── src/main/
    ├── java/com/tradeflow/
    │   ├── TradeflowApplication.java
    │   ├── config/
    │   │   ├── KafkaConfig.java             ← Consumer factory, manual ack setup
    │   │   └── WebConfig.java               ← Global CORS (REST + GraphQL)
    │   ├── controller/
    │   │   └── TradeController.java         ← REST endpoints
    │   ├── graphql/
    │   │   └── TradeGraphQLController.java  ← @QueryMapping handlers
    │   ├── model/
    │   │   ├── TradeEvent.java              ← Kafka message model
    │   │   ├── TradeResponse.java           ← Shared REST + GraphQL DTO
    │   │   ├── TradeBySymbol.java           ← Cassandra entity
    │   │   ├── TradeByTrader.java           ← Cassandra entity
    │   │   └── TradeById.java               ← Cassandra entity
    │   ├── producer/
    │   │   └── TradeEventProducer.java
    │   ├── consumer/
    │   │   ├── TradeEventConsumer.java
    │   │   └── TradeValidator.java
    │   ├── repository/
    │   │   ├── TradeBySymbolRepository.java
    │   │   ├── TradeByTraderRepository.java
    │   │   └── TradeByIdRepository.java
    │   └── service/
    │       ├── TradePersistenceService.java
    │       └── TradeQueryService.java
    └── resources/
        ├── application.yml
        └── graphql/
            └── schema.graphqls              ← GraphQL type definitions
```

---

## 🧪 Testing the Full Pipeline

### Submit a trade

```bash
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
```

### Query via REST

```bash
curl http://localhost:8080/api/trades/symbol/AAPL
curl http://localhost:8080/api/trades/trader/trader-001
curl http://localhost:8080/api/trades/{tradeId}
```

### Query via GraphQL curl

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ tradesBySymbol(symbol: \"AAPL\") { tradeId symbol side status tradeValue } }"}'
```

### Verify Cassandra directly

```bash
docker exec -it tradeflow-cassandra cqlsh -e \
  "SELECT trade_id, symbol, side, quantity, price, status
   FROM tradeflow.trades_by_symbol WHERE symbol='AAPL';"
```

### Monitor Kafka topic live

```bash
docker exec tradeflow-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic trade-events \
  --from-beginning
```

---

## 💡 Key Concepts & Interview Talking Points

### Kafka Partition Key Strategy

Trade events are keyed by `symbol`. All events for the same instrument land on the same partition, guaranteeing processing order per symbol. Up to 3 trades for different symbols are processed simultaneously on different consumer threads with zero coordination.

### CQRS — Command Query Responsibility Segregation

Write and read paths are completely decoupled. The API returns `202 Accepted` immediately on submission. The consumer processes asynchronously. The read side queries Cassandra directly — no coupling to the write path.

### GraphQL Field Selection

The GraphQL schema enforces types at the boundary — enums like `Side` and `TradeStatus` prevent invalid values at the type system level. Clients request exactly the fields they need in each query. The risk desk, compliance team, and settlement desk each get their own projection of the same trade data with a single endpoint.

### Manual Kafka Acknowledgment

Offset commits only after successful validation and persistence. If the service crashes mid-processing, Kafka redelivers. No trade is ever silently lost — critical in financial systems where missing a message is a compliance violation.

### Cassandra Query-Driven Modeling

Three access patterns require three tables. The same trade is written to all three simultaneously. Results come back newest-first automatically via `CLUSTERING ORDER BY (timestamp DESC)` — no application-level sorting needed.

### Global CORS for REST + GraphQL

`@CrossOrigin` on individual controllers does not apply to Spring for GraphQL's servlet. A `WebMvcConfigurer` with `addCorsMappings("/**")` is the correct pattern to cover all transports from a single configuration.

---

## 🔧 Useful Commands

```bash
# List Kafka topics
docker exec tradeflow-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Describe consumer group lag
docker exec tradeflow-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group tradeflow-consumer-group

# Open Cassandra shell
docker exec -it tradeflow-cassandra cqlsh

# Restart a single Docker service
docker compose restart kafka

# Full reset — removes all persisted data
docker compose down -v && docker compose up -d

# React clean install
cd tradeflow-ui
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
npm start
```

---

## 🚧 Known Limitations (Dev Environment)

- `replication-factor: 1` — single Kafka broker, no redundancy. Use 3 in production.
- Cassandra `SimpleStrategy` replication — use `NetworkTopologyStrategy` in production.
- No authentication on Kafka or Cassandra — add SASL/SSL for production.
- Apollo Client 3.x requires `--legacy-peer-deps` with React 19.
- React UI refreshes on a fixed delay after trade submission — replace with GraphQL Subscription + WebSocket for true real-time push.
- No GraphQL mutations — trade submission still goes through the REST endpoint.

---

## 🗺️ Suggested Next Steps

| Enhancement | Concepts Covered |
|---|---|
| GraphQL Mutation — submit trades | Complete GraphQL write path |
| GraphQL Subscription | Real-time trade push over WebSocket |
| Kafka Dead Letter Topic | Permanent failure handling, ops alerting |
| Dockerize Spring Boot app | Add app container to docker-compose.yml |
| Trade position aggregator | Net position per symbol, P&L calculation |
| Spring Security + JWT | API authentication, token-based auth |
| Deploy to AWS ECS | Connect TradeFlow to cloud infrastructure |
| Kafka Streams | Real-time aggregations — volume and value per symbol |

---

## 📚 References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Cassandra Documentation](https://cassandra.apache.org/doc/latest/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Spring for GraphQL](https://docs.spring.io/spring-graphql/docs/current/reference/html/)
- [Spring Data Cassandra](https://docs.spring.io/spring-data/cassandra/docs/current/reference/html/)
- [Apollo Client Documentation](https://www.apollographql.com/docs/react/)
- [React Router Documentation](https://reactrouter.com/en/main)
- [Confluent Platform Docker Images](https://hub.docker.com/u/confluentinc)

---
