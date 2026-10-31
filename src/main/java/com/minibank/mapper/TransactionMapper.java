package com.minibank.mapper;

import com.minibank.dto.response.ExpenseResponse;
import com.minibank.dto.response.TransactionResponse;
import com.minibank.entity.Transaction;
import com.minibank.repository.ExpenseByCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface TransactionMapper {

    @Mapping(target = "fromAccountNumber", source = "fromAccount.accountNumber")
    @Mapping(target = "toAccountNumber", source = "toAccount.accountNumber")
    @Mapping(target = "currency", source = "toAccount.currency")
    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toResponses(List<Transaction> transactions);

    List<ExpenseResponse> toExpenseResponses(List<ExpenseByCategory> expenses);
}
