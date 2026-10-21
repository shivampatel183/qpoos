package com.qpoos.erp.accounting.ledger.infrastructure;

import com.qpoos.erp.accounting.ledger.domain.LedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntity, Long> {
    boolean existsByCompany_IdAndCodeIgnoreCase(UUID companyId, String code);
    boolean existsByCompany_IdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, Long id);
    Optional<LedgerEntity> findByIdAndCompany_Id(Long id, UUID companyId);
    Optional<LedgerEntity> findByCompany_IdAndCodeIgnoreCase(UUID companyId, String code);
    List<LedgerEntity> findAllByCompany_IdAndActiveTrueOrderByNameAsc(UUID companyId);
    List<LedgerEntity> findAllByCompany_Id(UUID companyId);
    boolean existsByAccountGroup_AccountTypeDefinition_IdAndActiveTrue(Long definitionId);
}
