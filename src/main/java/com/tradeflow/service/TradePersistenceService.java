package com.tradeflow.service;

import com.tradeflow.model.*;
import com.tradeflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradePersistenceService {

    private final TradeBySymbolRepository bySymbolRepo;
    private final TradeByTraderRepository byTraderRepo;
    private final TradeByIdRepository byIdRepo;

    public void persist(TradeEvent trade) {
        UUID tradeId = UUID.fromString(trade.getTradeId());

        // Write to all three tables simultaneously
        bySymbolRepo.save(TradeBySymbol.builder()
                .symbol(trade.getSymbol())
                .timestamp(trade.getTimestamp())
                .tradeId(tradeId)
                .traderId(trade.getTraderId())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .exchange(trade.getExchange())
                .build());

        byTraderRepo.save(TradeByTrader.builder()
                .traderId(trade.getTraderId())
                .timestamp(trade.getTimestamp())
                .tradeId(tradeId)
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .exchange(trade.getExchange())
                .build());

        byIdRepo.save(TradeById.builder()
                .tradeId(tradeId)
                .traderId(trade.getTraderId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .exchange(trade.getExchange())
                .timestamp(trade.getTimestamp())
                .build());

        log.info("Trade persisted to Cassandra — ID: {}, Status: {}",
                trade.getTradeId(), trade.getStatus());
    }
}