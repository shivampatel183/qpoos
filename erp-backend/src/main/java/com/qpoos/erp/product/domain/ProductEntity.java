package com.qpoos.erp.product.domain;

import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_company_sku",     columnNames = {"company_id", "sku"}),
                @UniqueConstraint(name = "uk_product_company_barcode", columnNames = {"company_id", "barcode"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    // ── identity ────────────────────────────────────────────────────────────

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    // ── references ──────────────────────────────────────────────────────────

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "category_id")
    private UUID categoryId;

    // ── codes ───────────────────────────────────────────────────────────────

    @Column(length = 100)
    private String barcode;

    @Column(length = 100)
    private String sku;

    @Column(name = "hsn_code", length = 10)
    private String hsnCode;

    // ── inventory tracking ──────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "track_inventory", nullable = false)
    private Boolean trackInventory = false;

    @Builder.Default
    @Column(name = "quantity_on_hand", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "as_of_date")
    private LocalDate asOfDate;

    @Builder.Default
    @Column(name = "opening_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal openingQuantity = BigDecimal.ZERO;

    @Column(name = "opening_value", precision = 18, scale = 2)
    private BigDecimal openingValue;

    @Column(name = "alert_quantity", precision = 18, scale = 3)
    private BigDecimal alertQuantity;

    @Column(name = "reorder_quantity", precision = 18, scale = 3)
    private BigDecimal reorderQuantity;

    // ── sales ────────────────────────────────────────────────────────────────

    @Column(name = "sale_price", precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "sale_tax_id")
    private UUID saleTaxId;

    // ── purchase ─────────────────────────────────────────────────────────────

    @Column(name = "purchase_cost", precision = 18, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "purchase_tax_id")
    private UUID purchaseTaxId;

    // ── audit ────────────────────────────────────────────────────────────────

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
