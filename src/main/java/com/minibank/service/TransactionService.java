package com.minibank.service;

import com.minibank.dto.request.DepositRequest;
import com.minibank.dto.request.TransferRequest;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.Account;
import com.minibank.entity.Transaction;
import com.minibank.entity.TransactionCategory;
import com.minibank.exception.BusinessRuleException;
import com.minibank.exception.ConflictException;
import com.minibank.exception.NotFoundException;
import com.minibank.mapper.TransactionMapper;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse deposit(Long userId, Long accountId, DepositRequest request, UUID idempotencyKey) {
        Account account = lockAccount(accountId);
        checkOwner(account, userId);

        Optional<TransactionResponse> previous = findPrevious(idempotencyKey, account);
        if (previous.isPresent()) {
            return previous.get();
        }

        account.deposit(request.amount());
        Transaction transaction = Transaction.deposit(account, request.amount(), request.description(), idempotencyKey);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse transfer(Long userId, TransferRequest request, UUID idempotencyKey) {
        Long fromId = request.fromAccountId();
        Long toId = accountRepository.findIdByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account " + request.toAccountNumber() + " not found"));
        if (fromId.equals(toId)) {
            throw new BusinessRuleException("Cannot transfer to the same account");
        }

        // Always lock in ascending id order: otherwise A->B and B->A running together can deadlock
        Account first = lockAccount(Math.min(fromId, toId));
        Account second = lockAccount(Math.max(fromId, toId));
        Account from = first.getId().equals(fromId) ? first : second;
        Account to = from == first ? second : first;
        checkOwner(from, userId);

        Optional<TransactionResponse> previous = findPrevious(idempotencyKey, from);
        if (previous.isPresent()) {
            return previous.get();
        }

        if (from.getCurrency() != to.getCurrency()) {
            throw new BusinessRuleException("Transfers are allowed only between accounts in the same currency");
        }

        from.withdraw(request.amount());
        to.deposit(request.amount());

        TransactionCategory category = request.category() != null ? request.category() : TransactionCategory.OTHER;
        Transaction transaction = Transaction.transfer(from, to, request.amount(), category, request.description(),
                idempotencyKey);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    private Account lockAccount(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Account " + accountId + " not found"));
    }

    private void checkOwner(Account account, Long userId) {
        if (!account.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Account " + account.getId() + " not found");
        }
    }

    // Checked after the accounts are locked: a retry of the same request waits here until the first one commits
    private Optional<TransactionResponse> findPrevious(UUID idempotencyKey, Account account) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(previous -> {
                    if (!previous.involves(account)) {
                        throw new ConflictException("Idempotency key is already used for another operation");
                    }
                    return transactionMapper.toResponse(previous);
                });
    }
}
