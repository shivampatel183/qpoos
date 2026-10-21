package com.qpoos.erp.common.term;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTermRepository extends JpaRepository<PaymentTermEntity, Long> {

    boolean existsByCompany_IdAndCodeIgnoreCase(UUID companyId, String code);

    boolean existsByCompany_IdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, Long id);

    Optional<PaymentTermEntity> findByIdAndCompany_Id(Long id, UUID companyId);

    List<PaymentTermEntity> findAllByCompany_IdAndActiveTrueOrderByDueDaysAscNameAsc(UUID companyId);
}