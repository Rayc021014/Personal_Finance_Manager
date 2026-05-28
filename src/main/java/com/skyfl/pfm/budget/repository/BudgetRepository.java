package com.skyfl.pfm.budget.repository;

import com.skyfl.pfm.budget.entity.Budget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndYearAndMonthOrderByCreatedAtAsc(UUID userId, short year, short month);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(UUID userId, UUID categoryId, short year, short month);
}
