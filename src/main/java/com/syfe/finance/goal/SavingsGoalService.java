package com.syfe.finance.goal;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.category.CategoryType;
import com.syfe.finance.common.ApiException;
import com.syfe.finance.common.MessageResponse;
import com.syfe.finance.transaction.FinancialTransaction;
import com.syfe.finance.transaction.TransactionRepository;
import com.syfe.finance.user.UserAccount;
import com.syfe.finance.user.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public SavingsGoalService(
            SavingsGoalRepository savingsGoalRepository,
            TransactionRepository transactionRepository,
            UserService userService
    ) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional
    public GoalResponse create(CurrentUser currentUser, CreateGoalRequest request) {
        UserAccount user = userService.requireUser(currentUser);
        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        validateTargetDate(startDate, request.targetDate());
        SavingsGoal goal = new SavingsGoal(
                request.goalName().trim(),
                request.targetAmount(),
                request.targetDate(),
                startDate,
                user
        );
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public GoalsResponse getGoals(CurrentUser currentUser) {
        UserAccount user = userService.requireUser(currentUser);
        List<GoalResponse> goals = savingsGoalRepository.findByUserOrderByIdAsc(user).stream()
                .map(this::toResponse)
                .toList();
        return new GoalsResponse(goals);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoal(CurrentUser currentUser, Long id) {
        UserAccount user = userService.requireUser(currentUser);
        return toResponse(findOwnedGoal(id, user));
    }

    @Transactional
    public GoalResponse update(CurrentUser currentUser, Long id, UpdateGoalRequest request) {
        UserAccount user = userService.requireUser(currentUser);
        SavingsGoal goal = findOwnedGoal(id, user);
        if (request.goalName() != null && !request.goalName().isBlank()) {
            goal.setGoalName(request.goalName().trim());
        }
        if (request.targetAmount() != null) {
            goal.setTargetAmount(request.targetAmount());
        }
        if (request.targetDate() != null) {
            validateTargetDate(goal.getStartDate(), request.targetDate());
            goal.setTargetDate(request.targetDate());
        }
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public MessageResponse delete(CurrentUser currentUser, Long id) {
        UserAccount user = userService.requireUser(currentUser);
        SavingsGoal goal = findOwnedGoal(id, user);
        savingsGoalRepository.delete(goal);
        return new MessageResponse("Goal deleted successfully");
    }

    private SavingsGoal findOwnedGoal(Long id, UserAccount user) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Goal belongs to another user");
        }
        return goal;
    }

    private void validateTargetDate(LocalDate startDate, LocalDate targetDate) {
        if (!targetDate.isAfter(LocalDate.now())) {
            throw ApiException.badRequest("Target date must be in the future");
        }
        if (!targetDate.isAfter(startDate)) {
            throw ApiException.badRequest("Target date must be after the goal start date");
        }
    }

    private GoalResponse toResponse(SavingsGoal goal) {
        BigDecimal progress = calculateProgress(goal);
        BigDecimal percentage = goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : percent(progress, goal.getTargetAmount());
        BigDecimal remaining = goal.getTargetAmount().subtract(progress);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                money(goal.getTargetAmount()),
                goal.getTargetDate(),
                goal.getStartDate(),
                money(progress),
                percentage,
                money(remaining)
        );
    }

    private BigDecimal calculateProgress(SavingsGoal goal) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinancialTransaction transaction : transactionRepository.findByUserAndDateGreaterThanEqual(goal.getUser(), goal.getStartDate())) {
            if (transaction.getCategory().getType() == CategoryType.INCOME) {
                total = total.add(transaction.getAmount());
            } else {
                total = total.subtract(transaction.getAmount());
            }
        }
        return total;
    }

    private BigDecimal money(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal progress, BigDecimal targetAmount) {
        BigDecimal percentage = progress.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        if (percentage.scale() < 1) {
            percentage = percentage.setScale(1, RoundingMode.HALF_UP);
        }
        return percentage;
    }
}
