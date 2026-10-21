package com.qpoos.erp.accounting.accountcatalog.infrastructure;

import com.qpoos.erp.accounting.accountcatalog.domain.AccountTypeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountTypeDefinitionRepository extends JpaRepository<AccountTypeDefinitionEntity, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<AccountTypeDefinitionEntity> findByCodeIgnoreCase(String code);
    List<AccountTypeDefinitionEntity> findAllByActiveTrueOrderByAccountGroup_DisplayOrderAscDisplayOrderAscNameAsc();
}
