package com.qpoos.erp.common.number;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentNumberSequenceRepository extends JpaRepository<DocumentNumberSequenceEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sequence
            from DocumentNumberSequenceEntity sequence
            where sequence.company.id = :companyId
              and sequence.documentType = :documentType
              and sequence.financialYear = :financialYear
            """)
    Optional<DocumentNumberSequenceEntity> findForUpdate(
            @Param("companyId") UUID companyId,
            @Param("documentType") String documentType,
            @Param("financialYear") String financialYear
    );
}