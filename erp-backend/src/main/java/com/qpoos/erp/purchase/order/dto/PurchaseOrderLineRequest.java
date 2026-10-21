package com.qpoos.erp.purchase.order.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PurchaseOrderLineRequest(
        @NotNull @Positive Integer lineNo,
        @NotNull Long productId,
        @Size(max = 2000) String description,
        @NotNull @Positive @Digits(integer = 15, fraction = 3) BigDecimal quantity,
        @Size(max = 50) String unit,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal rate,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal discount,
        @PositiveOrZero @Digits(integer = 3, fraction = 2) BigDecimal taxRate
) {
}