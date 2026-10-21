package com.qpoos.erp.accounting.accounttype.infrastructure;

import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountTypeRepository extends JpaRepository<AccountTypeEntity, Long> {
    Optional<AccountTypeEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<AccountTypeEntity> findAllByOrderByDisplayOrderAsc();
}
