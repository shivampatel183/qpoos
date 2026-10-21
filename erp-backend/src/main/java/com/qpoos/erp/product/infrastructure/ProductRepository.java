package com.qpoos.erp.product.infrastructure;

import com.qpoos.erp.product.domain.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsByCompany_IdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompany_IdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, Long id);

    boolean existsByCompany_IdAndSkuIgnoreCase(UUID companyId, String sku);

    boolean existsByCompany_IdAndSkuIgnoreCaseAndIdNot(UUID companyId, String sku, Long id);

    boolean existsByCompany_IdAndBarcodeIgnoreCase(UUID companyId, String barcode);

    boolean existsByCompany_IdAndBarcodeIgnoreCaseAndIdNot(UUID companyId, String barcode, Long id);

    Optional<ProductEntity> findByIdAndCompany_IdAndActiveTrue(Long id, UUID companyId);

    List<ProductEntity> findAllByCompany_IdAndActiveTrueOrderByNameAsc(UUID companyId);
}
