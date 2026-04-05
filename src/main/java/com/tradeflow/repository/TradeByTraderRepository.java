package com.tradeflow.repository;

import com.tradeflow.model.TradeByTrader;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeByTraderRepository
        extends CassandraRepository<TradeByTrader, UUID> {
    List<TradeByTrader> findByTraderId(String traderId);
}