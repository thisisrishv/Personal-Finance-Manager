package com.syfe.finance.category;

import com.syfe.finance.user.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByOwnerAndNameIgnoreCase(UserAccount owner, String name);

    Optional<Category> findByOwnerIsNullAndNameIgnoreCase(String name);

    boolean existsByOwnerAndNameIgnoreCase(UserAccount owner, String name);

    boolean existsByOwnerIsNullAndNameIgnoreCase(String name);

    @Query("select c from Category c where c.owner is null or c.owner = :owner order by c.id asc")
    List<Category> findAccessibleCategories(@Param("owner") UserAccount owner);
}
