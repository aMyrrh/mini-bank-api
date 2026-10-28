package com.minibank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Currency {
    RUB("810"),
    USD("840"),
    EUR("978");

    private final String numericCode;
}
