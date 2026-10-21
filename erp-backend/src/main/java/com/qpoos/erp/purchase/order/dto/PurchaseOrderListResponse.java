package com.qpoos.erp.purchase.order.dto;

import com.qpoos.erp.purchase.shared.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderListResponse(
        Long id,
        String orderNumber,
        LocalDate orderDate,
        Long vendorId,
        String vendorName,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        LocalDate expectedDeliveryDate
) {
}