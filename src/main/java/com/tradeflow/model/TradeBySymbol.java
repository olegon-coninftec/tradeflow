package com.tradeflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trades_by_symbol")
public class TradeBySymbol {

    @PrimaryKeyColumn(name = "symbol", type = PrimaryKeyType.PARTITIONED)
    private String symbol;

    @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING)
    private Instant timestamp;

    @PrimaryKeyColumn(name = "trade_id", type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.ASCENDING)
    private UUID tradeId;

    @Column("trader_id")  private String traderId;
    @Column("side")       private String side;
    @Column("quantity")   private Integer quantity;
    @Column("price")      private BigDecimal price;
    @Column("status")     private String status;
    @Column("exchange")   private String exchange;
}