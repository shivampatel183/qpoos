package com.qpoos.erp.common.number;

import com.qpoos.erp.common.financialyear.FinancialYearService;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private final DocumentNumberSequenceRepository sequenceRepository;
    private final EntityManager entityManager;
    private final FinancialYearService financialYearService;

    @Transactional
    public String next(DocumentNumberType type) {
        return next(type, LocalDate.now());
    }

    @Transactional
    public String next(DocumentNumberType type, LocalDate accountingDate) {
        UUID companyId = SecurityUtils.getCompanyId();
        if (accountingDate == null) {
            throw new IllegalArgumentException("Accounting date is required");
        }
        CompanyEntity company = entityManager.find(
                CompanyEntity.class,
                companyId,
                LockModeType.PESSIMISTIC_WRITE
        );
        if (company == null) {
            throw new IllegalArgumentException("Company not found");
        }
        String financialYear = financialYearService.forCompanyDate(company, accountingDate).code();
        DocumentNumberSequenceEntity sequence = sequenceRepository
                .findForUpdate(companyId, type.name(), financialYear)
                .orElseGet(() -> createSequence(company, type, financialYear));

        long currentNumber = sequence.getNextNumber();
        sequence.setNextNumber(currentNumber + 1);
        sequenceRepository.save(sequence);
        return format(type, financialYear, currentNumber);
    }

    String format(DocumentNumberType type, String financialYear, long number) {
        return "%s-%s-%05d".formatted(type.prefix(), financialYear, number);
    }

    private DocumentNumberSequenceEntity createSequence(
            CompanyEntity company,
            DocumentNumberType type,
            String financialYear
    ) {
        return sequenceRepository.save(
                DocumentNumberSequenceEntity.builder()
                        .company(company)
                        .documentType(type.name())
                        .financialYear(financialYear)
                        .nextNumber(1L)
                        .build()
        );
    }
}