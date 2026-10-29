package com.minibank.controller;

import com.minibank.dto.request.DepositRequest;
import com.minibank.dto.request.TransferRequest;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/api/accounts/{id}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(@RequestHeader("X-User-Id") Long userId,
                                       @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                       @PathVariable Long id,
                                       @Valid @RequestBody DepositRequest request) {
        return transactionService.deposit(userId, id, request, idempotencyKey);
    }

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("Idempotency-Key") UUID idempotencyKey,
                                        @Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(userId, request, idempotencyKey);
    }
}
