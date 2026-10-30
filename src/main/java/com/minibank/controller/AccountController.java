package com.minibank.controller;

import com.minibank.dto.request.CreateAccountRequest;
import com.minibank.dto.response.AccountResponse;
import com.minibank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> open(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.open(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(account.id())
                .toUri();
        return ResponseEntity.created(location).body(account);
    }

    @GetMapping
    public List<AccountResponse> getAll(@AuthenticationPrincipal Long userId) {
        return accountService.getAll(userId);
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return accountService.getById(userId, id);
    }

    @PostMapping("/{id}/close")
    public AccountResponse close(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return accountService.close(userId, id);
    }
}
