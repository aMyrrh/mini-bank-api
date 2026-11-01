package com.minibank.entity;

import com.minibank.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private final User owner = new User("user@example.com", "hash", "Test User", Role.USER);
    private final Account account = new Account("40817810000000000001", owner, Currency.RUB);

    @Test
    void newAccountIsActiveWithZeroBalance() {
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void depositIncreasesBalance() {
        account.deposit(new BigDecimal("100.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("100.50");
    }

    @Test
    void withdrawDecreasesBalance() {
        account.deposit(new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("30.25"));

        assertThat(account.getBalance()).isEqualByComparingTo("69.75");
    }

    @Test
    void withdrawWholeBalanceIsAllowed() {
        account.deposit(new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void withdrawMoreThanBalanceFailsAndKeepsBalance() {
        account.deposit(new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.01")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void closedAccountRejectsDepositAndWithdraw() {
        account.close();

        assertThatThrownBy(() -> account.deposit(BigDecimal.ONE)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> account.withdraw(BigDecimal.ONE)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void closeSetsClosedStatus() {
        account.close();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void accountWithMoneyCannotBeClosed() {
        account.deposit(BigDecimal.ONE);

        assertThatThrownBy(account::close).isInstanceOf(BusinessRuleException.class);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void closedAccountCannotBeClosedAgain() {
        account.close();

        assertThatThrownBy(account::close).isInstanceOf(BusinessRuleException.class);
    }
}
