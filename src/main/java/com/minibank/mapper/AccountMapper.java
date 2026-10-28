package com.minibank.mapper;

import com.minibank.dto.response.AccountResponse;
import com.minibank.entity.Account;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface AccountMapper {

    AccountResponse toResponse(Account account);

    List<AccountResponse> toResponses(List<Account> accounts);
}
