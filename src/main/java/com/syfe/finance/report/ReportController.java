package com.syfe.finance.report;

import com.syfe.finance.auth.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ReportResponse monthly(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable int year,
            @PathVariable int month
    ) {
        return reportService.monthly(currentUser, year, month);
    }

    @GetMapping("/yearly/{year}")
    public YearlyReportResponse yearly(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable int year
    ) {
        return reportService.yearly(currentUser, year);
    }
}
