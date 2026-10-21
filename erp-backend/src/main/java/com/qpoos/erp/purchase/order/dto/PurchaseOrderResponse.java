package com.qpoos.erp.purchase.order.dto;

import com.qpoos.erp.purchase.shared.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String orderNumber,
        LocalDate orderDate,
        Long vendorId,
        String vendorName,
        Long paymentTermId,
        String paymentTermCode,
        String deliveryTerm,
        LocalDate expectedDeliveryDate,
        DeliveryAddressRequest deliveryAddress,
        PurchaseOrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal roundOff,
        BigDecimal totalAmount,
        String notes,
        String attachments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PurchaseOrderLineResponse> lines
) {
}