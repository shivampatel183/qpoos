package com.qpoos.erp.purchase.bill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseBillRequest(
        @NotNull LocalDate billDate,
        @NotNull Long vendorId,
        Long paymentTermId,
        LocalDate dueDate,
        Long purchaseOrderId,
        BigDecimal roundOff,
        @Size(max = 4000) String notes,
        @Size(max = 4000) String attachments,
        @NotEmpty List<@Valid PurchaseBillLineRequest> lines
) {
}
