package com.syfe.finance.goal;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.common.MessageResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/goals", "/goals"})
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateGoalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsGoalService.create(currentUser, request));
    }

    @GetMapping
    public GoalsResponse getGoals(@AuthenticationPrincipal CurrentUser currentUser) {
        return savingsGoalService.getGoals(currentUser);
    }

    @GetMapping("/{id}")
    public GoalResponse getGoal(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        return savingsGoalService.getGoal(currentUser, id);
    }

    @PutMapping("/{id}")
    public GoalResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        return savingsGoalService.update(currentUser, id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        return savingsGoalService.delete(currentUser, id);
    }
}
