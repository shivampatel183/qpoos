package com.qpoos.erp.purchase.bill.dto;

import java.math.BigDecimal;

public record PurchaseBillLineResponse(
        Long id,
        Integer lineNo,
        Long productId,
        String productName,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal rate,
        BigDecimal discount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal amount
) {
}
