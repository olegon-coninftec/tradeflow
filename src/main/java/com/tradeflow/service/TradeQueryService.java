package com.tradeflow.service;

import com.tradeflow.model.*;
import com.tradeflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeQueryService {

    private final TradeByIdRepository byIdRepo;
    private final TradeBySymbolRepository bySymbolRepo;
    private final TradeByTraderRepository byTraderRepo;

    public Optional<TradeResponse> findById(String tradeId) {
        return byIdRepo.findById(UUID.fromString(tradeId))
                .map(this::fromTradeById);
    }

    public List<TradeResponse> findBySymbol(String symbol) {
        return bySymbolRepo.findBySymbol(symbol.toUpperCase())
                .stream()
                .map(this::fromTradeBySymbol)
                .collect(Collectors.toList());
    }

    public List<TradeResponse> findByTrader(String traderId) {
        return byTraderRepo.findByTraderId(traderId)
                .stream()
                .map(this::fromTradeByTrader)
                .collect(Collectors.toList());
    }

    private TradeResponse fromTradeById(TradeById t) {
        return TradeResponse.builder()
                .tradeId(t.getTradeId())
                .traderId(t.getTraderId())
                .symbol(t.getSymbol())
                .side(t.getSide())
                .quantity(t.getQuantity())
                .price(t.getPrice())
                .tradeValue(t.getPrice().multiply(
                        new BigDecimal(t.getQuantity())))
                .status(t.getStatus())
                .exchange(t.getExchange())
                .timestamp(t.getTimestamp())
                .build();
    }

    private TradeResponse fromTradeBySymbol(TradeBySymbol t) {
        return TradeResponse.builder()
                .tradeId(t.getTradeId())
                .traderId(t.getTraderId())
                .symbol(t.getSymbol())
                .side(t.getSide())
                .quantity(t.getQuantity())
                .price(t.getPrice())
                .tradeValue(t.getPrice().multiply(
                        new BigDecimal(t.getQuantity())))
                .status(t.getStatus())
                .exchange(t.getExchange())
                .timestamp(t.getTimestamp())
                .build();
    }

    private TradeResponse fromTradeByTrader(TradeByTrader t) {
        return TradeResponse.builder()
                .tradeId(t.getTradeId())
                .traderId(t.getTraderId())
                .symbol(t.getSymbol())
                .side(t.getSide())
                .quantity(t.getQuantity())
                .price(t.getPrice())
                .tradeValue(t.getPrice().multiply(
                        new BigDecimal(t.getQuantity())))
                .status(t.getStatus())
                .exchange(t.getExchange())
                .timestamp(t.getTimestamp())
                .build();
    }
}