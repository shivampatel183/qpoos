package com.qpoos.erp.purchase.bill.dto;

import com.qpoos.erp.purchase.shared.domain.PurchaseBillStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseBillListResponse(
        Long id,
        String billNumber,
        LocalDate billDate,
        Long vendorId,
        String vendorName,
        PurchaseBillStatus status,
        BigDecimal totalAmount,
        LocalDate dueDate
) {
}
