package com.qpoos.erp.vendor.infrastructure;

import com.qpoos.erp.vendor.domain.VendorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<VendorEntity, Long> {
    boolean existsByCompany_IdAndDisplayNameIgnoreCase(UUID companyId, String displayName);
    boolean existsByCompany_IdAndDisplayNameIgnoreCaseAndIdNot(
            UUID companyId,
            String displayName,
            Long id
    );
    Optional<VendorEntity> findByIdAndCompany_IdAndActiveTrue(Long id, UUID companyId);
    List<VendorEntity> findAllByCompany_IdAndActiveTrueOrderByDisplayNameAsc(UUID companyId);
}
