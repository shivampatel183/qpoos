package com.qpoos.erp.common.number;

import com.qpoos.erp.common.financialyear.FinancialYearService;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private final DocumentNumberSequenceRepository sequenceRepository;
    private final EntityManager entityManager;
    private final FinancialYearService financialYearService;

    @Transactional
    public String next(DocumentNumberType type) {
        UUID companyId = SecurityUtils.getCompanyId();
        String financialYear = financialYearService.current().code();
        DocumentNumberSequenceEntity sequence = sequenceRepository
                .findForUpdate(companyId, type.name(), financialYear)
                .orElseGet(() -> createSequence(companyId, type, financialYear));

        long currentNumber = sequence.getNextNumber();
        sequence.setNextNumber(currentNumber + 1);
        sequenceRepository.save(sequence);
        return format(type, financialYear, currentNumber);
    }

    String format(DocumentNumberType type, String financialYear, long number) {
        return "%s-%s-%05d".formatted(type.prefix(), financialYear, number);
    }

    private DocumentNumberSequenceEntity createSequence(
            UUID companyId,
            DocumentNumberType type,
            String financialYear
    ) {
        return sequenceRepository.save(
                DocumentNumberSequenceEntity.builder()
                        .company(entityManager.getReference(CompanyEntity.class, companyId))
                        .documentType(type.name())
                        .financialYear(financialYear)
                        .nextNumber(1L)
                        .build()
        );
    }
}