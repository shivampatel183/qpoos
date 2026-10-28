package com.qpoos.erp.payment.infrastructure;

import com.qpoos.erp.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.qpoos.erp.payment.dto.PaymentListResponse;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    @Query(value = """
            select new com.qpoos.erp.payment.dto.PaymentListResponse(
                p.id, p.paymentNumber, p.paymentDate, p.vendor.id, p.vendor.displayName,
                p.paymentMethod, p.amount, p.status,
                (select count(a.id) from PaymentAllocationEntity a where a.payment.id = p.id)
            )
            from PaymentEntity p
            where p.company.id = :companyId
              and (:search is null
                or lower(p.paymentNumber) like lower(concat('%', :search, '%'))
                or lower(p.vendor.displayName) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.referenceNumber, '')) like lower(concat('%', :search, '%')))
            """, countQuery = """
            select count(p)
            from PaymentEntity p
            where p.company.id = :companyId
              and (:search is null
                or lower(p.paymentNumber) like lower(concat('%', :search, '%'))
                or lower(p.vendor.displayName) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.referenceNumber, '')) like lower(concat('%', :search, '%')))
            """)
    Page<PaymentListResponse> findPaymentSummaries(@Param("companyId") UUID companyId,
                                                    @Param("search") String search,
                                                    Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.id = :id and p.company.id = :companyId")
    Optional<PaymentEntity> findForPosting(@Param("id") Long id, @Param("companyId") UUID companyId);
}
