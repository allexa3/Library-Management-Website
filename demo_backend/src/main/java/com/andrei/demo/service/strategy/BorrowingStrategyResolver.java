package com.andrei.demo.service.strategy;

import com.andrei.demo.model.UserRole;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class BorrowingStrategyResolver {

    private final List<BorrowingStrategy> strategies;

    public BorrowingStrategyResolver(List<BorrowingStrategy> strategies) {
        this.strategies = strategies;
    }

    public BorrowingStrategy getStrategy(UserRole role) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No borrowing strategy found for role: " + role));
    }
}