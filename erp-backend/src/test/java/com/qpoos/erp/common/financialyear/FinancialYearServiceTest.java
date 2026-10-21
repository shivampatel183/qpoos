package com.qpoos.erp.common.financialyear;

import com.qpoos.erp.company.domain.CompanyEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class FinancialYearServiceTest {

    private final FinancialYearService service = new FinancialYearService(mock());

    @Test
    void calculatesAprilToMarchFinancialYear() {
        CompanyEntity company = company("2025-04-01", "2026-03-31");

        FinancialYearService.FinancialYear financialYear = service.forCompanyDate(
                company,
                LocalDate.of(2026, 9, 20)
        );

        assertThat(financialYear.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(financialYear.endDate()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(financialYear.code()).isEqualTo("2026-27");
    }

    @Test
    void usesPreviousStartYearBeforeConfiguredStartDate() {
        CompanyEntity company = company("2025-04-01", "2026-03-31");

        FinancialYearService.FinancialYear financialYear = service.forCompanyDate(
                company,
                LocalDate.of(2026, 2, 10)
        );

        assertThat(financialYear.startDate()).isEqualTo(LocalDate.of(2025, 4, 1));
        assertThat(financialYear.endDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(financialYear.code()).isEqualTo("2025-26");
    }

    @Test
    void rejectsInvalidConfiguredPeriod() {
        CompanyEntity company = company("2025-04-01", "2026-04-01");

        assertThatIllegalArgumentException().isThrownBy(() ->
                service.forCompanyDate(company, LocalDate.of(2026, 9, 20))
        );
    }

    private CompanyEntity company(String start, String end) {
        return CompanyEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("Test Company")
                .financialYearStart(start)
                .financialYearEnd(end)
                .build();
    }
}