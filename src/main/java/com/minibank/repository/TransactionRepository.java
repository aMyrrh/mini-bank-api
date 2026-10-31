package com.minibank.repository;

import com.minibank.entity.Transaction;
import com.minibank.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey);

    // countQuery is explicit: Spring Data cannot derive it from a query with join fetch
    @Query(value = """
            select t from Transaction t
            left join fetch t.fromAccount
            join fetch t.toAccount
            where (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)
              and (:type is null or t.type = :type)
              and t.createdAt >= :from and t.createdAt < :to
            order by t.createdAt desc, t.id desc
            """,
            countQuery = """
            select count(t) from Transaction t
            where (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)
              and (:type is null or t.type = :type)
              and t.createdAt >= :from and t.createdAt < :to
            """)
    Page<Transaction> findHistory(Long accountId, TransactionType type, Instant from, Instant to, Pageable pageable);

    @Query("""
            select t from Transaction t
            left join fetch t.fromAccount
            join fetch t.toAccount
            where (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)
              and t.createdAt >= :from and t.createdAt < :to
            order by t.createdAt, t.id
            """)
    List<Transaction> findForStatement(Long accountId, Instant from, Instant to);

    @Query("""
            select coalesce(sum(case when t.toAccount.id = :accountId then t.amount else 0 end), 0) as income,
                   coalesce(sum(case when t.fromAccount.id = :accountId then t.amount else 0 end), 0) as outcome
            from Transaction t
            where (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)
              and t.createdAt >= :from and t.createdAt < :to
            """)
    AccountTurnover sumTurnover(Long accountId, Instant from, Instant to);

    // Transfers between the user's own accounts are not expenses, deposits have no from_account_id
    @Query(value = """
            select t.category as category, fa.currency as currency, sum(t.amount) as total, count(*) as count
            from transactions t
            join accounts fa on fa.id = t.from_account_id
            join accounts ta on ta.id = t.to_account_id
            where fa.user_id = :userId
              and ta.user_id <> :userId
              and t.created_at >= :from and t.created_at < :to
            group by t.category, fa.currency
            order by fa.currency, total desc
            """, nativeQuery = true)
    List<ExpenseByCategory> findExpensesByCategory(Long userId, Instant from, Instant to);
}
