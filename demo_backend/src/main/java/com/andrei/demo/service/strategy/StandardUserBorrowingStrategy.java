package com.andrei.demo.service.strategy;

import com.andrei.demo.model.UserRole;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class StandardUserBorrowingStrategy implements BorrowingStrategy {

    @Override
    public boolean supports(UserRole role) {
        return role == UserRole.CUSTOMER;
    }

    @Override
    public LocalDate calculateDueDate(LocalDate borrowDate) {
        // Standard users get 14 days to return a book
        return borrowDate.plusDays(14);
    }
}