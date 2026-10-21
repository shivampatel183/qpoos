package com.qpoos.erp.purchase.order.infrastructure;

import com.qpoos.erp.purchase.order.domain.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    Optional<PurchaseOrderEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    List<PurchaseOrderEntity> findAllByCompany_IdOrderByOrderDateDescIdDesc(UUID companyId);
}