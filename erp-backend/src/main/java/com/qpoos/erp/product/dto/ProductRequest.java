package com.qpoos.erp.product.dto;

import com.qpoos.erp.product.domain.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductRequest(

        // ── identity ──────────────────────────────────────────────────────────

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        ProductType type,

        // ── references ────────────────────────────────────────────────────────

        UUID unitId,

        UUID categoryId,

        // ── codes ─────────────────────────────────────────────────────────────

        @Size(max = 100)
        String barcode,

        @Size(max = 100)
        String sku,

        @Size(max = 10)
        String hsnCode,

        // ── inventory tracking ────────────────────────────────────────────────

        Boolean trackInventory,

        @DecimalMin(value = "0.000")
        @Digits(integer = 15, fraction = 3)
        BigDecimal quantityOnHand,

        LocalDate asOfDate,

        @DecimalMin(value = "0.000")
        @Digits(integer = 15, fraction = 3)
        BigDecimal openingQuantity,

        @DecimalMin(value = "0.00")
        @Digits(integer = 16, fraction = 2)
        BigDecimal openingValue,

        @DecimalMin(value = "0.000")
        @Digits(integer = 15, fraction = 3)
        BigDecimal alertQuantity,

        @DecimalMin(value = "0.000")
        @Digits(integer = 15, fraction = 3)
        BigDecimal reorderQuantity,

        // ── sales ─────────────────────────────────────────────────────────────

        @DecimalMin(value = "0.00")
        @Digits(integer = 16, fraction = 2)
        BigDecimal salePrice,

        UUID saleTaxId,

        // ── purchase ──────────────────────────────────────────────────────────

        @DecimalMin(value = "0.00")
        @Digits(integer = 16, fraction = 2)
        BigDecimal purchaseCost,

        UUID purchaseTaxId
) {
}
