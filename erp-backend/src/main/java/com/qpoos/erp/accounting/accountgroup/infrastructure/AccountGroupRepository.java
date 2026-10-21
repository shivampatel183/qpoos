package com.qpoos.erp.accounting.accountgroup.infrastructure;

import com.qpoos.erp.accounting.accountgroup.domain.AccountGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountGroupRepository extends JpaRepository<AccountGroupEntity, Long> {
    boolean existsByCompany_IdAndCodeIgnoreCase(UUID companyId, String code);
    boolean existsByCompany_IdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, Long id);
    Optional<AccountGroupEntity> findByIdAndCompany_Id(Long id, UUID companyId);
    Optional<AccountGroupEntity> findByCompany_IdAndCodeIgnoreCase(UUID companyId, String code);
    Optional<AccountGroupEntity> findFirstByCompany_IdAndAccountTypeDefinition_IdOrderByIdAsc(
            UUID companyId,
            Long definitionId
    );
    List<AccountGroupEntity> findAllByCompany_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(UUID companyId);
    List<AccountGroupEntity> findAllByCompany_Id(UUID companyId);
}
