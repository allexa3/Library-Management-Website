package com.andrei.demo.service.strategy;

import com.andrei.demo.model.UserRole;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class AdminBorrowingStrategy implements BorrowingStrategy {

    @Override
    public boolean supports(UserRole role) {
        return role == UserRole.ADMIN;
    }

    @Override
    public LocalDate calculateDueDate(LocalDate borrowDate) {
        // Admins get an extended period of 30 days
        return borrowDate.plusDays(30);
    }
}