package com.tradeflow.repository;

import com.tradeflow.model.TradeById;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TradeByIdRepository
        extends CassandraRepository<TradeById, UUID> {
}