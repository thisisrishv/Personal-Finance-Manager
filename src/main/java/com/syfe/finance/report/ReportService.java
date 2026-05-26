package com.syfe.finance.report;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.category.CategoryType;
import com.syfe.finance.common.ApiException;
import com.syfe.finance.transaction.FinancialTransaction;
import com.syfe.finance.transaction.TransactionRepository;
import com.syfe.finance.user.UserAccount;
import com.syfe.finance.user.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public ReportService(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ReportResponse monthly(CurrentUser currentUser, int year, int month) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw ApiException.badRequest("Month must be between 1 and 12");
        }
        UserAccount user = userService.requireUser(currentUser);
        YearMonth yearMonth = YearMonth.of(year, month);
        ReportTotals totals = calculate(user, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        return new ReportResponse(month, year, totals.income(), totals.expenses(), totals.netSavings());
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse yearly(CurrentUser currentUser, int year) {
        validateYear(year);
        UserAccount user = userService.requireUser(currentUser);
        ReportTotals totals = calculate(user, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return new YearlyReportResponse(year, totals.income(), totals.expenses(), totals.netSavings());
    }

    private ReportTotals calculate(UserAccount user, LocalDate startDate, LocalDate endDate) {
        List<FinancialTransaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);
        Map<String, BigDecimal> income = new LinkedHashMap<>();
        Map<String, BigDecimal> expenses = new LinkedHashMap<>();
        BigDecimal netSavings = BigDecimal.ZERO;
        for (FinancialTransaction transaction : transactions) {
            String categoryName = transaction.getCategory().getName();
            BigDecimal amount = transaction.getAmount();
            if (transaction.getCategory().getType() == CategoryType.INCOME) {
                income.merge(categoryName, amount, BigDecimal::add);
                netSavings = netSavings.add(amount);
            } else {
                expenses.merge(categoryName, amount, BigDecimal::add);
                netSavings = netSavings.subtract(amount);
            }
        }
        return new ReportTotals(scale(income), scale(expenses), money(netSavings));
    }

    private Map<String, BigDecimal> scale(Map<String, BigDecimal> source) {
        Map<String, BigDecimal> scaled = new LinkedHashMap<>();
        source.forEach((key, value) -> scaled.put(key, money(value)));
        return scaled;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 3000) {
            throw ApiException.badRequest("Year is out of supported range");
        }
    }

    private record ReportTotals(
            Map<String, BigDecimal> income,
            Map<String, BigDecimal> expenses,
            BigDecimal netSavings
    ) {
    }
}
