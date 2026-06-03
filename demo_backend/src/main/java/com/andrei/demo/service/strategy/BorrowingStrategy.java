package com.andrei.demo.service.strategy;

import com.andrei.demo.model.UserRole;
import java.time.LocalDate;

public interface BorrowingStrategy {
    boolean supports(UserRole role);
    LocalDate calculateDueDate(LocalDate borrowDate);
}