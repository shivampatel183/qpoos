package com.qpoos.erp.purchase.bill.infrastructure;

import com.qpoos.erp.purchase.bill.domain.PurchaseBillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseBillRepository extends JpaRepository<PurchaseBillEntity, Long> {

    Optional<PurchaseBillEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    List<PurchaseBillEntity> findAllByCompany_IdOrderByBillDateDescIdDesc(UUID companyId);
}
