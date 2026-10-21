package com.qpoos.erp.purchase.transaction.dto;

import com.qpoos.erp.purchase.transaction.domain.PaymentStatus;
import com.qpoos.erp.purchase.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        Long id,
        UUID companyId,
        LocalDate transactionDate,
        TransactionType transactionType,
        String billNo,
        Long vendorId,
        LocalDate dueDate,
        String referenceNo,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal roundOff,
        BigDecimal totalAmount,
        LocalDate paymentDate,
        PaymentStatus paymentStatus,
        String notes,
        String attachments,
        UUID createdBy,
        UUID updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TransactionLineResponse> lines
) {
}
