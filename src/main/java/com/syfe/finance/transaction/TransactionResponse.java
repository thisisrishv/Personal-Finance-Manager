package com.syfe.finance.transaction;

import com.syfe.finance.category.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        LocalDate date,
        String category,
        String description,
        CategoryType type
) {
}
