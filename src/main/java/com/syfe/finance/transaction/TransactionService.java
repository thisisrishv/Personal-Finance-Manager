package com.syfe.finance.transaction;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.category.Category;
import com.syfe.finance.category.CategoryService;
import com.syfe.finance.category.CategoryType;
import com.syfe.finance.common.ApiException;
import com.syfe.finance.common.MessageResponse;
import com.syfe.finance.user.UserAccount;
import com.syfe.finance.user.UserService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserService userService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryService categoryService,
            UserService userService
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @Transactional
    public TransactionResponse create(CurrentUser currentUser, CreateTransactionRequest request) {
        UserAccount user = userService.requireUser(currentUser);
        Category category = categoryService.findAccessibleByName(user, request.category());
        FinancialTransaction transaction = new FinancialTransaction(
                request.amount(),
                request.date(),
                category,
                normalizeDescription(request.description()),
                user
        );
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionsResponse getTransactions(
            CurrentUser currentUser,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String type
    ) {
        UserAccount user = userService.requireUser(currentUser);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw ApiException.badRequest("Start date cannot be after end date");
        }
        Category category = categoryId == null ? null : categoryService.findAccessibleById(user, categoryId);
        CategoryType parsedType = parseType(type);
        List<FinancialTransaction> transactions = transactionRepository.findAll(specification(user, startDate, endDate, category, parsedType));
        return new TransactionsResponse(transactions.stream().map(this::toResponse).toList());
    }

    @Transactional
    public TransactionResponse update(CurrentUser currentUser, Long id, UpdateTransactionRequest request) {
        UserAccount user = userService.requireUser(currentUser);
        FinancialTransaction transaction = findOwnedTransaction(id, user);
        if (request.date() != null) {
            throw ApiException.badRequest("Transaction date cannot be updated");
        }
        if (request.amount() != null) {
            transaction.setAmount(request.amount());
        }
        if (request.category() != null) {
            transaction.setCategory(categoryService.findAccessibleByName(user, request.category()));
        }
        if (request.description() != null) {
            transaction.setDescription(normalizeDescription(request.description()));
        }
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public MessageResponse delete(CurrentUser currentUser, Long id) {
        UserAccount user = userService.requireUser(currentUser);
        FinancialTransaction transaction = findOwnedTransaction(id, user);
        transactionRepository.delete(transaction);
        return new MessageResponse("Transaction deleted successfully");
    }

    private FinancialTransaction findOwnedTransaction(Long id, UserAccount user) {
        FinancialTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Transaction belongs to another user");
        }
        return transaction;
    }

    private Specification<FinancialTransaction> specification(
            UserAccount user,
            LocalDate startDate,
            LocalDate endDate,
            Category category,
            CategoryType type
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user"), user));
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("type"), type));
            }
            query.orderBy(criteriaBuilder.desc(root.get("date")), criteriaBuilder.desc(root.get("id")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private CategoryType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return CategoryType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("Transaction type must be INCOME or EXPENSE");
        }
    }

    private TransactionResponse toResponse(FinancialTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getCategory().getName(),
                transaction.getDescription(),
                transaction.getCategory().getType()
        );
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
