package com.syfe.finance.goal;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGoalRequest(
        @Size(max = 255, message = "Goal name cannot exceed 255 characters")
        String goalName,

        @Positive(message = "Target amount must be positive")
        BigDecimal targetAmount,

        LocalDate targetDate
) {
}
