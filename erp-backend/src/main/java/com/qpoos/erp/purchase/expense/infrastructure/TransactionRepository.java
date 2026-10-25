package com.qpoos.erp.purchase.expense.infrastructure;

import com.qpoos.erp.purchase.expense.domain.TransactionEntity;
import com.qpoos.erp.purchase.expense.domain.TransactionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByIdAndCompany_IdAndActiveTrue(Long id, UUID companyId);

    List<TransactionEntity> findAllByCompany_IdAndActiveTrueOrderByTransactionDateDescIdDesc(UUID companyId);

    List<TransactionEntity> findAllByCompany_IdAndTransactionTypeAndActiveTrueOrderByTransactionDateDescIdDesc(
            UUID companyId,
            TransactionType transactionType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select transaction
            from TransactionEntity transaction
            where transaction.id = :id
              and transaction.company.id = :companyId
              and transaction.active = true
            """)
    Optional<TransactionEntity> findForPosting(
            @Param("id") Long id,
            @Param("companyId") UUID companyId
    );
}
