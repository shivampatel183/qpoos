package com.qpoos.erp.purchase.bill.dto;

import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PurchaseBillResponse(
        Long id,
        String billNumber,
        LocalDate billDate,
        Long vendorId,
        String vendorName,
        Long paymentTermId,
        String paymentTermCode,
        LocalDate dueDate,
        Long purchaseOrderId,
        PurchaseBillStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal roundOff,
        BigDecimal totalAmount,
        Long postedVoucherId,
        String notes,
        String attachments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PurchaseBillLineResponse> lines
) {
}
