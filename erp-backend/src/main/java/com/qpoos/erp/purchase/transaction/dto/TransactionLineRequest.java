package com.qpoos.erp.purchase.transaction.dto;

import com.qpoos.erp.purchase.transaction.domain.TransactionLineType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionLineRequest(
        @NotNull
        @Positive
        Integer lineNo,

        @NotNull
        TransactionLineType lineType,

        Long itemId,

        Long accountId,

        @Size(max = 2000)
        String description,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 15, fraction = 3)
        BigDecimal quantity,

        @Size(max = 50)
        String unit,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal rate,

        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal discount,

        @DecimalMin(value = "0.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal taxRate,

        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal taxAmount,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}
