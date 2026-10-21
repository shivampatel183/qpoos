package com.qpoos.erp.purchase.bill.infrastructure;

import com.qpoos.erp.purchase.bill.domain.PurchaseBillLineEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseBillLineRepository extends JpaRepository<PurchaseBillLineEntity, Long> {

    List<PurchaseBillLineEntity> findAllByPurchaseBill_IdOrderByLineNoAsc(Long purchaseBillId);

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query("delete from PurchaseBillLineEntity line where line.purchaseBill.id = :purchaseBillId")
    void deleteAllByPurchaseBillId(@Param("purchaseBillId") Long purchaseBillId);
}
