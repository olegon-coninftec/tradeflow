package com.tradeflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trades_by_id")
public class TradeById {

    @PrimaryKey("trade_id")
    private UUID tradeId;

    @Column("trader_id") private String traderId;
    @Column("symbol")    private String symbol;
    @Column("side")      private String side;
    @Column("quantity")  private Integer quantity;
    @Column("price")     private BigDecimal price;
    @Column("status")    private String status;
    @Column("exchange")  private String exchange;
    @Column("timestamp") private Instant timestamp;
}