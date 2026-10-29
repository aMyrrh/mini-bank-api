package com.minibank.mapper;

import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TransactionMapper {

    @Mapping(target = "fromAccountNumber", source = "fromAccount.accountNumber")
    @Mapping(target = "toAccountNumber", source = "toAccount.accountNumber")
    @Mapping(target = "currency", source = "toAccount.currency")
    TransactionResponse toResponse(Transaction transaction);
}
