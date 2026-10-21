package com.qpoos.erp.purchase.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull LocalDate orderDate,
        @NotNull Long vendorId,
        Long paymentTermId,
        @Size(max = 120) String deliveryTerm,
        LocalDate expectedDeliveryDate,
        @Valid DeliveryAddressRequest deliveryAddress,
        BigDecimal roundOff,
        @Size(max = 4000) String notes,
        @Size(max = 4000) String attachments,
        @NotEmpty List<@Valid PurchaseOrderLineRequest> lines
) {
}