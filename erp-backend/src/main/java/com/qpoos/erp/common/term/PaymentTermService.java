package com.qpoos.erp.common.term;

import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTermService {

    private final PaymentTermRepository paymentTermRepository;
    private final EntityManager entityManager;

    @Transactional
    public PaymentTermResponse create(PaymentTermRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        String code = normalizeCode(request.code());
        if (paymentTermRepository.existsByCompany_IdAndCodeIgnoreCase(companyId, code)) {
            throw new IllegalArgumentException("Payment term code already exists");
        }

        UUID userId = SecurityUtils.getUserId();
        PaymentTermEntity term = PaymentTermEntity.builder()
                .company(entityManager.getReference(CompanyEntity.class, companyId))
                .code(code)
                .name(request.name().trim())
                .dueDays(request.dueDays())
                .active(request.active() == null || request.active())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        return toResponse(paymentTermRepository.save(term));
    }

    @Transactional
    public List<PaymentTermResponse> list() {
        return paymentTermRepository
                .findAllByCompany_IdAndActiveTrueOrderByDueDaysAscNameAsc(SecurityUtils.getCompanyId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentTermResponse update(Long id, PaymentTermRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        PaymentTermEntity term = paymentTermRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Payment term not found"));
        String code = normalizeCode(request.code());
        if (paymentTermRepository.existsByCompany_IdAndCodeIgnoreCaseAndIdNot(companyId, code, id)) {
            throw new IllegalArgumentException("Payment term code already exists");
        }

        term.setCode(code);
        term.setName(request.name().trim());
        term.setDueDays(request.dueDays());
        term.setActive(request.active() == null || request.active());
        term.setUpdatedBy(SecurityUtils.getUserId());
        return toResponse(paymentTermRepository.save(term));
    }

    private PaymentTermResponse toResponse(PaymentTermEntity term) {
        return new PaymentTermResponse(
                term.getId(),
                term.getCode(),
                term.getName(),
                term.getDueDays(),
                term.getActive()
        );
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}