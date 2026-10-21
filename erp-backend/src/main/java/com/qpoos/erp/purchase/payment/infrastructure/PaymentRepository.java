package com.qpoos.erp.purchase.payment.infrastructure;

import com.qpoos.erp.purchase.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    List<PaymentEntity> findAllByCompany_IdOrderByPaymentDateDescIdDesc(UUID companyId);
}