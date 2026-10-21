package com.qpoos.erp.accounting.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LedgerRequest(
        @NotNull
        Long accountTypeId,

        @NotBlank
        @Size(max = 180)
        String name,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal openingBalance,

        @NotNull
        LocalDate openingBalanceAsOfDate,

        @Size(max = 1000)
        String description
) {
}
