package com.qpoos.erp.purchase.expense.dto;

import com.qpoos.erp.purchase.expense.domain.PaymentStatus;
import com.qpoos.erp.purchase.expense.domain.TransactionType;

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
        Long paymentLedgerId,
        String paymentLedgerName,
        PaymentStatus paymentStatus,
        Long journalEntryId,
        String notes,
        String attachments,
        UUID createdBy,
        UUID updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TransactionLineResponse> lines
) {
}
