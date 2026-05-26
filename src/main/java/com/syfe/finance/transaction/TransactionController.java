package com.syfe.finance.transaction;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.common.MessageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(currentUser, request));
    }

    @GetMapping
    public TransactionsResponse getTransactions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type
    ) {
        return transactionService.getTransactions(currentUser, startDate, endDate, categoryId, type);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        return transactionService.update(currentUser, id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        return transactionService.delete(currentUser, id);
    }
}
