package com.qpoos.erp.purchase.order.infrastructure;

import com.qpoos.erp.purchase.order.domain.PurchaseOrderLineEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, Long> {

	List<PurchaseOrderLineEntity> findAllByPurchaseOrder_IdOrderByLineNoAsc(Long purchaseOrderId);

	@Modifying(flushAutomatically = true)
	@Transactional
	@Query("delete from PurchaseOrderLineEntity line where line.purchaseOrder.id = :purchaseOrderId")
	void deleteAllByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);
}