package com.qpoos.erp.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentAllocationRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 40) String documentType,
        @NotNull @Positive Long documentId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal allocatedAmount
) {}
