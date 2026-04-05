package com.tradeflow.consumer;

import com.tradeflow.model.TradeEvent;
import com.tradeflow.service.TradePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final TradeValidator validator;
    private final TradePersistenceService persistenceService;

    @KafkaListener(
            topics = "${tradeflow.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, TradeEvent> record,
                        Acknowledgment ack) {
        TradeEvent trade = record.value();

        log.info("=== Trade Received ===");
        log.info("Partition : {}", record.partition());
        log.info("Offset    : {}", record.offset());
        log.info("Trade ID  : {}", trade.getTradeId());
        log.info("Symbol    : {}", trade.getSymbol());
        log.info("Side      : {}", trade.getSide());
        log.info("Quantity  : {}", trade.getQuantity());
        log.info("Price     : ${}", trade.getPrice());
        log.info("Exchange  : {}", trade.getExchange());

        try {
            // Step 1 — Validate
            TradeValidator.ValidationResult result = validator.validate(trade);

            if (!result.valid()) {
                trade.setStatus("FAILED");
                log.warn("Trade FAILED validation — ID: {}, Errors: {}",
                        trade.getTradeId(), result.errors());
            } else {
                // Step 2 — Simulate execution (risk check, routing)
                simulateExecution(trade);
                trade.setStatus("EXECUTED");
                log.info("Trade EXECUTED — ID: {}, Value: ${}",
                        trade.getTradeId(),
                        trade.getPrice().multiply(
                                new BigDecimal(trade.getQuantity())));
            }

            // Step 3 — Persist to Cassandra
            persistenceService.persist(trade);

            // Step 4 — Acknowledge ONLY after successful processing
            ack.acknowledge();
            log.info("Offset committed — Trade: {}", trade.getTradeId());

        } catch (Exception e) {
            log.error("Failed to process trade: {} — offset NOT committed, will retry",
                    trade.getTradeId(), e);
            // Do NOT call ack.acknowledge() — Kafka will redeliver
        }

        log.info("=====================");
    }

    private void simulateExecution(TradeEvent trade) throws InterruptedException {
        // Simulate latency of exchange routing
        Thread.sleep(100);
        log.info("Trade routed to exchange: {}", trade.getExchange());
    }
}