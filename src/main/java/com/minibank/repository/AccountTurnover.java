package com.minibank.repository;

import java.math.BigDecimal;

public interface AccountTurnover {

    BigDecimal getIncome();

    BigDecimal getOutcome();
}
