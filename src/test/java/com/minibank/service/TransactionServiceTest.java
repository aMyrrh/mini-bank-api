package com.minibank.service;

import com.minibank.dto.request.TransferRequest;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.Account;
import com.minibank.entity.Currency;
import com.minibank.entity.Role;
import com.minibank.entity.Transaction;
import com.minibank.entity.TransactionCategory;
import com.minibank.entity.User;
import com.minibank.exception.BusinessRuleException;
import com.minibank.exception.ConflictException;
import com.minibank.exception.NotFoundException;
import com.minibank.mapper.TransactionMapper;
import com.minibank.mapper.TransactionMapperImpl;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final String TO_NUMBER = "40817810000000000002";
    private static final UUID KEY = UUID.randomUUID();

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Spy
    private TransactionMapper transactionMapper = new TransactionMapperImpl();
    @InjectMocks
    private TransactionService transactionService;

    @Test
    void transferMovesMoneyAndSavesTransaction() {
        Account from = account(1L, USER_ID, Currency.RUB, "100.00");
        Account to = account(2L, OTHER_USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        TransactionResponse response = transactionService.transfer(USER_ID, request(1L, "30.00"), KEY);

        assertThat(from.getBalance()).isEqualByComparingTo("70.00");
        assertThat(to.getBalance()).isEqualByComparingTo("30.00");
        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(response.category()).isEqualTo(TransactionCategory.OTHER);
        verify(transactionRepository).save(any());
    }

    @Test
    void accountsAreLockedInAscendingOrderWhenSenderHasLowerId() {
        Account from = account(1L, USER_ID, Currency.RUB, "100.00");
        Account to = account(2L, OTHER_USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        transactionService.transfer(USER_ID, request(1L, "1.00"), KEY);

        verifyLockOrder(1L, 2L);
    }

    @Test
    void accountsAreLockedInAscendingOrderWhenSenderHasHigherId() {
        Account from = account(2L, USER_ID, Currency.RUB, "100.00");
        Account to = account(1L, OTHER_USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        transactionService.transfer(USER_ID, request(2L, "1.00"), KEY);

        verifyLockOrder(1L, 2L);
    }

    @Test
    void transferToSameAccountIsRejectedBeforeLocking() {
        when(accountRepository.findIdByAccountNumber(TO_NUMBER)).thenReturn(Optional.of(1L));

        assertThatThrownBy(() -> transactionService.transfer(USER_ID, request(1L, "1.00"), KEY))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void transferToUnknownAccountIsRejected() {
        when(accountRepository.findIdByAccountNumber(TO_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer(USER_ID, request(1L, "1.00"), KEY))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void transferFromForeignAccountLooksLikeNotFound() {
        Account from = account(1L, OTHER_USER_ID, Currency.RUB, "100.00");
        Account to = account(2L, USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);

        assertThatThrownBy(() -> transactionService.transfer(USER_ID, request(1L, "1.00"), KEY))
                .isInstanceOf(NotFoundException.class);
        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transferBetweenDifferentCurrenciesIsRejected() {
        Account from = account(1L, USER_ID, Currency.RUB, "100.00");
        Account to = account(2L, OTHER_USER_ID, Currency.USD, "0.00");
        stubAccounts(from, to);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer(USER_ID, request(1L, "1.00"), KEY))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferWithInsufficientFundsSavesNothing() {
        Account from = account(1L, USER_ID, Currency.RUB, "10.00");
        Account to = account(2L, OTHER_USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer(USER_ID, request(1L, "10.01"), KEY))
                .isInstanceOf(BusinessRuleException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void repeatedKeyReturnsPreviousTransactionWithoutMovingMoney() {
        Account from = account(1L, USER_ID, Currency.RUB, "100.00");
        Account to = account(2L, OTHER_USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);
        Transaction previous = Transaction.transfer(from, to, new BigDecimal("30.00"), TransactionCategory.OTHER, null, KEY);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(previous));

        TransactionResponse response = transactionService.transfer(USER_ID, request(1L, "30.00"), KEY);

        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
        assertThat(to.getBalance()).isEqualByComparingTo("0.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void keyOfAnotherOperationIsRejected() {
        Account from = account(1L, USER_ID, Currency.RUB, "100.00");
        Account to = account(2L, OTHER_USER_ID, Currency.RUB, "0.00");
        stubAccounts(from, to);
        Account stranger = account(3L, OTHER_USER_ID, Currency.RUB, "0.00");
        Account strangerTarget = account(4L, OTHER_USER_ID, Currency.RUB, "0.00");
        Transaction previous = Transaction.transfer(stranger, strangerTarget, BigDecimal.ONE, TransactionCategory.OTHER, null, KEY);
        when(transactionRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> transactionService.transfer(USER_ID, request(1L, "1.00"), KEY))
                .isInstanceOf(ConflictException.class);
    }

    private void stubAccounts(Account from, Account to) {
        when(accountRepository.findIdByAccountNumber(TO_NUMBER)).thenReturn(Optional.of(to.getId()));
        when(accountRepository.findByIdForUpdate(from.getId())).thenReturn(Optional.of(from));
        when(accountRepository.findByIdForUpdate(to.getId())).thenReturn(Optional.of(to));
    }

    private void verifyLockOrder(Long firstId, Long secondId) {
        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(firstId);
        order.verify(accountRepository).findByIdForUpdate(secondId);
    }

    private TransferRequest request(Long fromId, String amount) {
        return new TransferRequest(fromId, TO_NUMBER, new BigDecimal(amount), null, null);
    }

    private Account account(Long id, Long ownerId, Currency currency, String balance) {
        User owner = new User("user" + ownerId + "@example.com", "hash", "Test User", Role.USER);
        ReflectionTestUtils.setField(owner, "id", ownerId);
        Account account = new Account("4081781000000000000" + id, owner, currency);
        ReflectionTestUtils.setField(account, "id", id);
        account.deposit(new BigDecimal(balance));
        return account;
    }
}
