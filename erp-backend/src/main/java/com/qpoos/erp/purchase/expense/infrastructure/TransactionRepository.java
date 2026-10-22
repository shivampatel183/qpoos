package com.qpoos.erp.purchase.expense.infrastructure;

import com.qpoos.erp.purchase.expense.domain.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByIdAndCompany_IdAndActiveTrue(Long id, UUID companyId);

    List<TransactionEntity> findAllByCompany_IdAndActiveTrueOrderByTransactionDateDescIdDesc(UUID companyId);
}
