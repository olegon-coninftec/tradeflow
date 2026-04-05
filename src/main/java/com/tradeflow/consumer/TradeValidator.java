package com.tradeflow.consumer;

import com.tradeflow.model.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class TradeValidator {

    private static final Set<String> VALID_SIDES = Set.of("BUY", "SELL");
    private static final Set<String> VALID_EXCHANGES = Set.of("NYSE", "NASDAQ", "LSE", "CME");
    private static final BigDecimal MAX_TRADE_VALUE = new BigDecimal("50000000"); // $50M limit

    public ValidationResult validate(TradeEvent trade) {
        List<String> errors = new ArrayList<>();

        if (trade.getSymbol() == null || trade.getSymbol().isBlank())
            errors.add("Symbol is required");

        if (trade.getSide() == null || !VALID_SIDES.contains(trade.getSide().toUpperCase()))
            errors.add("Side must be BUY or SELL");

        if (trade.getQuantity() == null || trade.getQuantity() <= 0)
            errors.add("Quantity must be positive");

        if (trade.getPrice() == null || trade.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            errors.add("Price must be positive");

        if (trade.getExchange() == null || !VALID_EXCHANGES.contains(trade.getExchange().toUpperCase()))
            errors.add("Exchange must be NYSE, NASDAQ, LSE, or CME");

        if (trade.getQuantity() != null && trade.getPrice() != null) {
            BigDecimal tradeValue = trade.getPrice()
                    .multiply(new BigDecimal(trade.getQuantity()));
            if (tradeValue.compareTo(MAX_TRADE_VALUE) > 0)
                errors.add("Trade value $" + tradeValue + " exceeds $50M limit");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}