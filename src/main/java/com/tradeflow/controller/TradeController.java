package com.tradeflow.controller;

import com.tradeflow.model.TradeEvent;
import com.tradeflow.model.TradeResponse;
import com.tradeflow.producer.TradeEventProducer;
import com.tradeflow.service.TradeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class TradeController {

    private final TradeEventProducer producer;
    private final TradeQueryService queryService;

    // WRITE side — submit trade to Kafka
    @PostMapping
    public ResponseEntity<TradeEvent> submitTrade(
            @RequestBody TradeEvent trade) {
        TradeEvent published = producer.submitTrade(trade);
        return ResponseEntity.accepted().body(published);
    }

    // READ side — query from Cassandra
    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> getById(
            @PathVariable String tradeId) {
        return queryService.findById(tradeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<TradeResponse>> getBySymbol(
            @PathVariable String symbol) {
        return ResponseEntity.ok(queryService.findBySymbol(symbol));
    }

    @GetMapping("/trader/{traderId}")
    public ResponseEntity<List<TradeResponse>> getByTrader(
            @PathVariable String traderId) {
        return ResponseEntity.ok(queryService.findByTrader(traderId));
    }
}