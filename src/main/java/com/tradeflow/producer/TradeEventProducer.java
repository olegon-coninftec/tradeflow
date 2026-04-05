package com.tradeflow.producer;

import com.tradeflow.model.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeEventProducer {

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;

    @Value("${tradeflow.kafka.topic}")
    private String topic;

    public TradeEvent submitTrade(TradeEvent trade) {
        // Assign system fields
        trade.setTradeId(UUID.randomUUID().toString());
        trade.setTimestamp(Instant.now());
        trade.setStatus("PENDING");

        // Use symbol as partition key — all trades for same symbol
        // go to the same partition, preserving order per symbol
        CompletableFuture<SendResult<String, TradeEvent>> future =
                kafkaTemplate.send(topic, trade.getSymbol(), trade);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Trade published — ID: {}, Symbol: {}, Partition: {}, Offset: {}",
                        trade.getTradeId(),
                        trade.getSymbol(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish trade: {}", trade.getTradeId(), ex);
            }
        });

        return trade;
    }
}