package com.qpoos.erp.payment.dto;

import com.qpoos.erp.payment.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentRequest(
        @NotNull LocalDate paymentDate,
        @NotNull Long vendorId,
        @NotNull PaymentMethod paymentMethod,
        @NotNull Long sourceLedgerId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Size(max = 100) String referenceNumber,
        @Size(max = 4000) String notes,
        @NotEmpty List<@Valid PaymentAllocationRequest> allocations
) {}
