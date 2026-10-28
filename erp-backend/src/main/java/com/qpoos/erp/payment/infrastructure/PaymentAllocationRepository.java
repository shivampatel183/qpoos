package com.qpoos.erp.payment.infrastructure;

import com.qpoos.erp.payment.domain.PaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocationEntity, Long> {
    List<PaymentAllocationEntity> findAllByPayment_IdAndCompany_Id(Long paymentId, UUID companyId);

    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(a.allocatedAmount), 0)
            from PaymentAllocationEntity a
            where a.company.id = :companyId
              and a.documentType = :documentType
              and a.documentId = :documentId
              and a.payment.status = com.qpoos.erp.payment.domain.PaymentStatus.POSTED
            """)
    java.math.BigDecimal sumPostedForDocument(
            @org.springframework.data.repository.query.Param("companyId") UUID companyId,
            @org.springframework.data.repository.query.Param("documentType") String documentType,
            @org.springframework.data.repository.query.Param("documentId") Long documentId
    );

    @org.springframework.data.jpa.repository.Query("""
            select a.documentId, sum(a.allocatedAmount)
            from PaymentAllocationEntity a
            where a.company.id = :companyId and a.documentType = :documentType
              and a.payment.status = com.qpoos.erp.payment.domain.PaymentStatus.POSTED
            group by a.documentId
            """)
    List<Object[]> sumPostedByDocument(@org.springframework.data.repository.query.Param("companyId") UUID companyId,
                                       @org.springframework.data.repository.query.Param("documentType") String documentType);

    List<PaymentAllocationEntity> findAllByDocumentTypeAndDocumentIdAndCompany_Id(
            String documentType, Long documentId, UUID companyId
    );

    void deleteAllByPayment_Id(Long paymentId);
}
