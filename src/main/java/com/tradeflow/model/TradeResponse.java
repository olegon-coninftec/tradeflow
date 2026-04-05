package com.tradeflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {
    private UUID tradeId;
    private String traderId;
    private String symbol;
    private String side;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal tradeValue;
    private String status;
    private String exchange;
    private Instant timestamp;
}