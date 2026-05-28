package com.skyfl.pfm.transaction.repository;

import com.skyfl.pfm.report.dto.CategoryBreakdownItem;
import com.skyfl.pfm.transaction.entity.Transaction;
import com.skyfl.pfm.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    boolean existsByUserIdAndCategoryIdAndDeletedFalse(UUID userId, UUID categoryId);

    @Query("""
            select coalesce(sum(
                case
                    when t.type = com.skyfl.pfm.transaction.entity.TransactionType.INCOME then t.amount
                    when t.type = com.skyfl.pfm.transaction.entity.TransactionType.EXPENSE then -t.amount
                    when t.type = com.skyfl.pfm.transaction.entity.TransactionType.TRANSFER
                         and lower(coalesce(t.note, '')) like 'transfer out%' then -t.amount
                    else t.amount
                end
            ), 0)
            from Transaction t
            where t.user.id = :userId and t.account.id = :accountId and t.deleted = false
            """)
    BigDecimal calculateNetAmount(UUID userId, UUID accountId);

    @Query("""
            select coalesce(sum(case when t.type = :type then t.amount else 0 end), 0)
            from Transaction t
            where t.user.id = :userId and t.deleted = false and t.transactionDate between :start and :end
            """)
    BigDecimal sumByType(UUID userId, TransactionType type, LocalDate start, LocalDate end);

    @Query("""
            select new com.skyfl.pfm.report.dto.CategoryBreakdownItem(
                t.category.id,
                coalesce(t.category.name, 'Uncategorized'),
                t.type,
                sum(t.amount)
            )
            from Transaction t
            where t.user.id = :userId
              and t.deleted = false
              and t.type in (
                  com.skyfl.pfm.transaction.entity.TransactionType.EXPENSE,
                  com.skyfl.pfm.transaction.entity.TransactionType.INCOME
              )
              and t.transactionDate between :start and :end
            group by t.category.id, t.category.name, t.type
            order by t.type, sum(t.amount) desc
            """)
    List<CategoryBreakdownItem> categoryBreakdown(UUID userId, LocalDate start, LocalDate end);

    @Query("""
            select t from Transaction t
            where t.user.id = :userId and t.deleted = false and t.transactionDate between :start and :end
            order by t.transactionDate desc, t.createdAt desc
            """)
    List<Transaction> exportRows(UUID userId, LocalDate start, LocalDate end);

    @Query("""
            select t from Transaction t
            where t.user.id = :userId and t.deleted = false and t.transactionDate between :start and :end
            order by t.transactionDate asc, t.createdAt asc
            """)
    List<Transaction> findAllForTrend(UUID userId, LocalDate start, LocalDate end);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.user.id = :userId
              and t.category.id = :categoryId
              and t.deleted = false
              and t.type = com.skyfl.pfm.transaction.entity.TransactionType.EXPENSE
              and t.transactionDate between :start and :end
            """)
    BigDecimal sumExpenseByCategory(UUID userId, UUID categoryId, LocalDate start, LocalDate end);
}
