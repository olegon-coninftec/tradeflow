package com.tradeflow.repository;

import com.tradeflow.model.TradeBySymbol;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeBySymbolRepository
        extends CassandraRepository<TradeBySymbol, UUID> {
    List<TradeBySymbol> findBySymbol(String symbol);
}