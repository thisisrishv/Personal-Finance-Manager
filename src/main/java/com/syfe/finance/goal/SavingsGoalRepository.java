package com.syfe.finance.goal;

import com.syfe.finance.user.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserOrderByIdAsc(UserAccount user);
}
