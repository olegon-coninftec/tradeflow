package com.tradeflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEvent {

    private String tradeId;
    private String traderId;
    private String symbol;          // e.g. AAPL, JPM, MSFT
    private String side;            // BUY or SELL
    private Integer quantity;
    private BigDecimal price;
    private String status;          // PENDING, EXECUTED, FAILED, CANCELLED
    private Instant timestamp;
    private String exchange;        // NYSE, NASDAQ, LSE
}