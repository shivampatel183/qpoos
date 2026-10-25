package com.qpoos.erp.purchase.expense.dto;

import com.qpoos.erp.purchase.expense.domain.PaymentStatus;
import com.qpoos.erp.purchase.expense.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionListResponse(
        Long id,
        LocalDate transactionDate,
        TransactionType transactionType,
        String billNo,
        Long vendorId,
        String vendorName,
        LocalDate dueDate,
        LocalDate paymentDate,
        Long paymentLedgerId,
        String paymentLedgerName,
        BigDecimal totalAmount,
        PaymentStatus paymentStatus,
        Long journalEntryId
) {
}
