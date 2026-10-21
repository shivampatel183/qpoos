package com.qpoos.erp.product.application;

import com.qpoos.erp.product.domain.ProductEntity;
import com.qpoos.erp.product.infrastructure.ProductRepository;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.product.dto.ProductRequest;
import com.qpoos.erp.product.dto.ProductResponse;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        CompanyEntity company = entityManager.getReference(CompanyEntity.class, companyId);

        String name = normalizeName(request.name());
        validateUniqueName(companyId, name, null);
        validateUniqueSku(companyId, uppercaseOrNull(request.sku()), null);
        validateUniqueBarcode(companyId, blankToNull(request.barcode()), null);
        ProductEntity product = ProductEntity.builder()
                .company(company)
                .active(true)
                .build();
        applyRequest(product, request, name);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public List<ProductResponse> list() {
        return productRepository
                .findAllByCompany_IdAndActiveTrueOrderByNameAsc(SecurityUtils.getCompanyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse get(Long productId) {
        return toResponse(getProduct(SecurityUtils.getCompanyId(), productId));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        ProductEntity product = getProduct(companyId, productId);

        String name = normalizeName(request.name());
        validateUniqueName(companyId, name, productId);
        validateUniqueSku(companyId, uppercaseOrNull(request.sku()), productId);
        validateUniqueBarcode(companyId, blankToNull(request.barcode()), productId);
        applyRequest(product, request, name);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long productId) {
        ProductEntity product = getProduct(SecurityUtils.getCompanyId(), productId);
        product.setActive(false);
        productRepository.save(product);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private ProductEntity getProduct(UUID companyId, Long productId) {
        return productRepository.findByIdAndCompany_IdAndActiveTrue(productId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    private void validateUniqueName(UUID companyId, String name, Long excludeId) {
        boolean exists = excludeId == null
                ? productRepository.existsByCompany_IdAndNameIgnoreCase(companyId, name)
                : productRepository.existsByCompany_IdAndNameIgnoreCaseAndIdNot(companyId, name, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Product name already exists");
        }
    }

    private void validateUniqueSku(UUID companyId, String sku, Long excludeId) {
        if (sku == null) return;
        boolean exists = excludeId == null
                ? productRepository.existsByCompany_IdAndSkuIgnoreCase(companyId, sku)
                : productRepository.existsByCompany_IdAndSkuIgnoreCaseAndIdNot(companyId, sku, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Product SKU already exists");
        }
    }

    private void validateUniqueBarcode(UUID companyId, String barcode, Long excludeId) {
        if (barcode == null) return;
        boolean exists = excludeId == null
                ? productRepository.existsByCompany_IdAndBarcodeIgnoreCase(companyId, barcode)
                : productRepository.existsByCompany_IdAndBarcodeIgnoreCaseAndIdNot(companyId, barcode, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Product barcode already exists");
        }
    }

    private void applyRequest(ProductEntity product, ProductRequest request, String name) {
        product.setName(name);
        product.setDescription(blankToNull(request.description()));
        product.setType(request.type());
        product.setUnitId(request.unitId());
        product.setCategoryId(request.categoryId());
        product.setBarcode(blankToNull(request.barcode()));
        product.setSku(uppercaseOrNull(request.sku()));
        product.setHsnCode(uppercaseOrNull(request.hsnCode()));
        product.setTrackInventory(request.trackInventory() != null && request.trackInventory());
        product.setQuantityOnHand(request.quantityOnHand() != null ? request.quantityOnHand() : BigDecimal.ZERO);
        product.setAsOfDate(request.asOfDate());
        product.setOpeningQuantity(request.openingQuantity() != null ? request.openingQuantity() : BigDecimal.ZERO);
        product.setOpeningValue(request.openingValue());
        product.setAlertQuantity(request.alertQuantity());
        product.setReorderQuantity(request.reorderQuantity());
        product.setSalePrice(request.salePrice());
        product.setSaleTaxId(request.saleTaxId());
        product.setPurchaseCost(request.purchaseCost());
        product.setPurchaseTaxId(request.purchaseTaxId());
    }

    private ProductResponse toResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getCompany().getId(),
                product.getName(),
                product.getDescription(),
                product.getType(),
                product.getUnitId(),
                product.getCategoryId(),
                product.getBarcode(),
                product.getSku(),
                product.getHsnCode(),
                product.getTrackInventory(),
                product.getQuantityOnHand(),
                product.getAsOfDate(),
                product.getOpeningQuantity(),
                product.getOpeningValue(),
                product.getAlertQuantity(),
                product.getReorderQuantity(),
                product.getSalePrice(),
                product.getSaleTaxId(),
                product.getPurchaseCost(),
                product.getPurchaseTaxId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String uppercaseOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
