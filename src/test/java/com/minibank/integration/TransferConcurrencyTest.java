package com.minibank.integration;

import com.minibank.dto.request.DepositRequest;
import com.minibank.dto.request.TransferRequest;
import com.minibank.dto.response.AccountResponse;
import com.minibank.entity.Currency;
import com.minibank.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.assertj.core.api.Assertions.assertThat;

// Not @Transactional: every thread must commit its own transaction for the others to see it
class TransferConcurrencyTest extends AbstractIntegrationTest {

    @Test
    void parallelTransfersNeverOverdraw() throws Exception {
        Long sender = createUser();
        Long receiver = createUser();
        AccountResponse from = openAccount(sender, Currency.RUB);
        AccountResponse to = openAccount(receiver, Currency.RUB);
        deposit(sender, from.id(), "100.00");
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runInParallel(10, i -> {
            try {
                transactionService.transfer(sender, transfer(from.id(), to.accountNumber(), "20.00"), UUID.randomUUID());
                succeeded.incrementAndGet();
            } catch (BusinessRuleException e) {
                rejected.incrementAndGet();
            }
        });

        assertThat(succeeded).hasValue(5);
        assertThat(rejected).hasValue(5);
        assertThat(balance(sender, from.id())).isEqualByComparingTo("0.00");
        assertThat(balance(receiver, to.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        Long firstUser = createUser();
        Long secondUser = createUser();
        AccountResponse first = openAccount(firstUser, Currency.RUB);
        AccountResponse second = openAccount(secondUser, Currency.RUB);
        deposit(firstUser, first.id(), "100.00");
        deposit(secondUser, second.id(), "100.00");

        runInParallel(10, i -> {
            if (i % 2 == 0) {
                transactionService.transfer(firstUser, transfer(first.id(), second.accountNumber(), "1.00"), UUID.randomUUID());
            } else {
                transactionService.transfer(secondUser, transfer(second.id(), first.accountNumber(), "1.00"), UUID.randomUUID());
            }
        });

        assertThat(balance(firstUser, first.id())).isEqualByComparingTo("100.00");
        assertThat(balance(secondUser, second.id())).isEqualByComparingTo("100.00");
    }

    @Test
    void sameIdempotencyKeyInParallelCreatesOneTransaction() throws Exception {
        Long user = createUser();
        AccountResponse account = openAccount(user, Currency.RUB);
        UUID key = UUID.randomUUID();
        List<Long> transactionIds = new CopyOnWriteArrayList<>();

        runInParallel(5, i -> transactionIds.add(transactionService
                .deposit(user, account.id(), new DepositRequest(new BigDecimal("10.00"), null), key).id()));

        assertThat(transactionIds).hasSize(5).containsOnly(transactionIds.get(0));
        assertThat(balance(user, account.id())).isEqualByComparingTo("10.00");
    }

    private TransferRequest transfer(Long fromId, String toNumber, String amount) {
        return new TransferRequest(fromId, toNumber, new BigDecimal(amount), null, null);
    }

    // The latch releases all threads at once to make them really compete for the same rows
    private void runInParallel(int threads, IntConsumer task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int number = i;
            Callable<Void> job = () -> {
                start.await();
                task.accept(number);
                return null;
            };
            results.add(executor.submit(job));
        }
        start.countDown();
        for (Future<Void> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
    }
}
