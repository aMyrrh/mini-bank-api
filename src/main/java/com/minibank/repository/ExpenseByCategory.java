package com.minibank.repository;

import com.minibank.entity.Currency;
import com.minibank.entity.TransactionCategory;

import java.math.BigDecimal;

public interface ExpenseByCategory {

    TransactionCategory getCategory();

    Currency getCurrency();

    BigDecimal getTotal();

    long getCount();
}
