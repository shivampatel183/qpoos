package com.qpoos.erp.purchase.payment.infrastructure;

import com.qpoos.erp.purchase.payment.domain.PaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocationEntity, Long> {
    List<PaymentAllocationEntity> findAllByPayment_IdAndCompany_Id(Long paymentId, UUID companyId);

    List<PaymentAllocationEntity> findAllByTransaction_IdAndCompany_Id(Long transactionId, UUID companyId);
}