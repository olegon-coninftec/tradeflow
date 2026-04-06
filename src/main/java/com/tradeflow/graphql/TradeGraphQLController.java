package com.tradeflow.graphql;

import com.tradeflow.model.TradeResponse;
import com.tradeflow.service.TradeQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class TradeGraphQLController {

    private final TradeQueryService queryService;

    @QueryMapping
    public TradeResponse tradeById(@Argument String tradeId) {
        log.info("GraphQL query — tradeById: {}", tradeId);
        return queryService.findById(tradeId).orElse(null);
    }

    @QueryMapping
    public List<TradeResponse> tradesBySymbol(@Argument String symbol) {
        log.info("GraphQL query — tradesBySymbol: {}", symbol);
        return queryService.findBySymbol(symbol);
    }

    @QueryMapping
    public List<TradeResponse> tradesByTrader(@Argument String traderId) {
        log.info("GraphQL query — tradesByTrader: {}", traderId);
        return queryService.findByTrader(traderId);
    }
}