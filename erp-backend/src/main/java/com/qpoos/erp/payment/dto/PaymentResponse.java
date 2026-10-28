package com.qpoos.erp.payment.dto;

import com.qpoos.erp.payment.domain.PaymentMethod;
import com.qpoos.erp.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        String paymentNumber,
        LocalDate paymentDate,
        Long vendorId,
        String vendorName,
        PaymentMethod paymentMethod,
        Long sourceLedgerId,
        String sourceLedgerName,
        BigDecimal amount,
        String referenceNumber,
        String notes,
        PaymentStatus status,
        Long voucherId,
        Long reversalVoucherId,
        String reversalReason,
        OffsetDateTime createdAt,
        List<Allocation> allocations
) {
    public record Allocation(Long id, String documentType, Long documentId, String documentNumber,
                             BigDecimal allocatedAmount, BigDecimal outstandingAmount) {}
}
