package com.minibank.service;

import com.minibank.dto.request.CreateAccountRequest;
import com.minibank.dto.response.AccountResponse;
import com.minibank.entity.Account;
import com.minibank.entity.Currency;
import com.minibank.entity.User;
import com.minibank.exception.NotFoundException;
import com.minibank.mapper.AccountMapper;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    // Balance account for individuals, as in real Russian account numbers
    private static final String INDIVIDUAL_ACCOUNT_PREFIX = "40817";
    private static final long TWELVE_DIGITS_BOUND = 1_000_000_000_000L;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AccountResponse open(Long userId, CreateAccountRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));

        Account account = new Account(generateAccountNumber(request.currency()), owner, request.currency());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAll(Long userId) {
        return accountMapper.toResponses(accountRepository.findAllByOwnerId(userId));
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(Long userId, Long accountId) {
        return accountMapper.toResponse(findOwnAccount(userId, accountId));
    }

    @Transactional
    public AccountResponse close(Long userId, Long accountId) {
        Account account = findOwnAccount(userId, accountId);
        account.close();
        return accountMapper.toResponse(account);
    }

    private Account findOwnAccount(Long userId, Long accountId) {
        return accountRepository.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account " + accountId + " not found"));
    }

    private String generateAccountNumber(Currency currency) {
        String number;
        do {
            number = INDIVIDUAL_ACCOUNT_PREFIX + currency.getNumericCode()
                    + "%012d".formatted(random.nextLong(TWELVE_DIGITS_BOUND));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
