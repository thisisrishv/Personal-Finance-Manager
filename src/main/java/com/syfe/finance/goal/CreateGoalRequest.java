package com.syfe.finance.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank(message = "Goal name is required")
        @Size(max = 255, message = "Goal name cannot exceed 255 characters")
        String goalName,

        @NotNull(message = "Target amount is required")
        @Positive(message = "Target amount must be positive")
        BigDecimal targetAmount,

        @NotNull(message = "Target date is required")
        LocalDate targetDate,

        LocalDate startDate
) {
}
