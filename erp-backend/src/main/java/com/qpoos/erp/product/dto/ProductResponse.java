package com.qpoos.erp.product.dto;

import com.qpoos.erp.product.domain.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        Long id,
        UUID companyId,

        // ── identity ──────────────────────────────────────────────────────────
        String name,
        String description,
        ProductType type,

        // ── references ────────────────────────────────────────────────────────
        UUID unitId,
        UUID categoryId,

        // ── codes ─────────────────────────────────────────────────────────────
        String barcode,
        String sku,
        String hsnCode,

        // ── inventory tracking ────────────────────────────────────────────────
        Boolean trackInventory,
        BigDecimal quantityOnHand,
        LocalDate asOfDate,
        BigDecimal openingQuantity,
        BigDecimal openingValue,
        BigDecimal alertQuantity,
        BigDecimal reorderQuantity,

        // ── sales ─────────────────────────────────────────────────────────────
        BigDecimal salePrice,
        UUID saleTaxId,

        // ── purchase ──────────────────────────────────────────────────────────
        BigDecimal purchaseCost,
        UUID purchaseTaxId,

        // ── audit ─────────────────────────────────────────────────────────────
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
