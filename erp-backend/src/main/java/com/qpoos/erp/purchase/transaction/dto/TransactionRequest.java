package com.qpoos.erp.purchase.transaction.dto;

import com.qpoos.erp.purchase.transaction.domain.PaymentStatus;
import com.qpoos.erp.purchase.transaction.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransactionRequest(
        @NotNull
        LocalDate transactionDate,

        @NotNull
        TransactionType transactionType,

        @Size(max = 50)
        String billNo,

        Long vendorId,

        LocalDate dueDate,

        @Size(max = 100)
        String referenceNo,

        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal subtotal,

        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal discountAmount,

        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal taxAmount,

        @Digits(integer = 17, fraction = 2)
        BigDecimal roundOff,

        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal totalAmount,

        LocalDate paymentDate,

        PaymentStatus paymentStatus,

        @Size(max = 4000)
        String notes,

        @Size(max = 4000)
        String attachments,

        @NotEmpty
        List<@Valid TransactionLineRequest> lines
) {
}
