package com.minibank.entity;

import com.minibank.exception.BusinessRuleException;
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

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, updatable = false)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, updatable = false)
    private Currency currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Account(String accountNumber, User owner, Currency currency) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.currency = currency;
        this.balance = new BigDecimal("0.00");
        this.status = AccountStatus.ACTIVE;
    }

    public void deposit(BigDecimal amount) {
        checkActive();
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        checkActive();
        if (balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds on account " + accountNumber);
        }
        balance = balance.subtract(amount);
    }

    private void checkActive() {
        if (status == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account " + accountNumber + " is closed");
        }
    }

    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account is already closed");
        }
        if (balance.signum() != 0) {
            throw new BusinessRuleException("Account with a non-zero balance cannot be closed");
        }
        status = AccountStatus.CLOSED;
    }
}
