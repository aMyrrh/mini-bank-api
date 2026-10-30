package com.minibank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TransactionCategory category;

    @Column(name = "description")
    private String description;

    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Transaction(TransactionType type, Account fromAccount, Account toAccount, BigDecimal amount,
                        TransactionCategory category, String description, UUID idempotencyKey) {
        this.type = type;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount.setScale(2);
        this.category = category;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }

    public static Transaction deposit(Account toAccount, BigDecimal amount, String description, UUID idempotencyKey) {
        return new Transaction(TransactionType.DEPOSIT, null, toAccount, amount, null, description, idempotencyKey);
    }

    public static Transaction transfer(Account fromAccount, Account toAccount, BigDecimal amount,
                                       TransactionCategory category, String description, UUID idempotencyKey) {
        return new Transaction(TransactionType.TRANSFER, fromAccount, toAccount, amount, category, description,
                idempotencyKey);
    }

    public boolean involves(Account account) {
        return toAccount.getId().equals(account.getId())
                || (fromAccount != null && fromAccount.getId().equals(account.getId()));
    }
}
