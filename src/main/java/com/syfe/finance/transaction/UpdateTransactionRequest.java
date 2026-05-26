package com.syfe.finance.transaction;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionRequest(
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String category,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @PastOrPresent(message = "Date cannot be in the future")
        LocalDate date
) {
}
