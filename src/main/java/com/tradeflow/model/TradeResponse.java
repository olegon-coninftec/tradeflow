package com.tradeflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    private Instant timestamp;

    // GraphQL reads this as the "timestamp" field (String scalar)
    public String getTimestamp() {
        return timestamp != null ? timestamp.toString() : null;
    }
}