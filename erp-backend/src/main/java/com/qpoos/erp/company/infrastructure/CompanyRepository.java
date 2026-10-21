package com.qpoos.erp.company.infrastructure;

import com.qpoos.erp.company.domain.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    List<CompanyEntity> findAllByIsActiveTrue();
    List<CompanyEntity> findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);
    Optional<CompanyEntity> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);
    boolean existsByUserIdAndNameIgnoreCaseAndIsActiveTrue(UUID userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndIsActiveTrue(UUID userId, String name, UUID id);
}
