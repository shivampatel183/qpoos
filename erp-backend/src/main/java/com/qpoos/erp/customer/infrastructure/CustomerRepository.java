package com.qpoos.erp.customer.infrastructure;

import com.qpoos.erp.customer.domain.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    boolean existsByCompany_IdAndDisplayNameIgnoreCase(UUID companyId, String displayName);

    boolean existsByCompany_IdAndDisplayNameIgnoreCaseAndIdNot(
            UUID companyId,
            String displayName,
            Long id
    );

    Optional<CustomerEntity> findByIdAndCompany_IdAndActiveTrue(Long id, UUID companyId);

    List<CustomerEntity> findAllByCompany_IdAndActiveTrueOrderByDisplayNameAsc(UUID companyId);
}
