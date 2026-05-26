package com.syfe.finance.transaction;

import java.util.List;

public record TransactionsResponse(List<TransactionResponse> transactions) {
}
