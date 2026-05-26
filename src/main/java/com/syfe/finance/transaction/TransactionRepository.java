package com.syfe.finance.transaction;

import com.syfe.finance.category.Category;
import com.syfe.finance.user.UserAccount;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    boolean existsByCategory(Category category);

    List<FinancialTransaction> findByUserAndDateGreaterThanEqual(UserAccount user, LocalDate startDate);

    List<FinancialTransaction> findByUserAndDateBetween(UserAccount user, LocalDate startDate, LocalDate endDate);
}
