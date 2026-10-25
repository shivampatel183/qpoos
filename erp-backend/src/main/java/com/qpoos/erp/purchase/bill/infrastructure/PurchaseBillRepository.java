package com.qpoos.erp.purchase.bill.infrastructure;

import com.qpoos.erp.purchase.bill.domain.PurchaseBillEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseBillRepository extends JpaRepository<PurchaseBillEntity, Long> {

    Optional<PurchaseBillEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    List<PurchaseBillEntity> findAllByCompany_IdOrderByBillDateDescIdDesc(UUID companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bill
            from PurchaseBillEntity bill
            where bill.id = :id and bill.company.id = :companyId
            """)
    Optional<PurchaseBillEntity> findForPosting(
            @Param("id") Long id,
            @Param("companyId") UUID companyId
    );
}
