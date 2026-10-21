package com.qpoos.erp.common.financialyear;

import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialYearService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CompanyRepository companyRepository;

    @Transactional
    public FinancialYear current() {
        return forDate(LocalDate.now());
    }

    @Transactional
    public FinancialYear forDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        UUID companyId = SecurityUtils.getCompanyId();
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        return forCompanyDate(company, date);
    }

    public FinancialYear forCompanyDate(CompanyEntity company, LocalDate date) {
        if (company == null || date == null) {
            throw new IllegalArgumentException("Company and date are required");
        }

        LocalDate configuredStart = parseDate(company.getFinancialYearStart(), "financial year start");
        LocalDate configuredEnd = parseDate(company.getFinancialYearEnd(), "financial year end");
        validateConfiguration(configuredStart, configuredEnd);

        MonthDay startMonthDay = MonthDay.from(configuredStart);
        int startYear = date.isBefore(startMonthDay.atYear(date.getYear()))
                ? date.getYear() - 1
                : date.getYear();
        LocalDate startDate = startMonthDay.atYear(startYear);
        LocalDate endDate = startDate.plusYears(1).minusDays(1);

        return new FinancialYear(
                startDate,
                endDate,
                "%d-%02d".formatted(startDate.getYear(), endDate.getYear() % 100)
        );
    }

    private void validateConfiguration(LocalDate start, LocalDate end) {
        LocalDate expectedEnd = start.plusYears(1).minusDays(1);
        if (!end.equals(expectedEnd)) {
            throw new IllegalArgumentException(
                    "Financial year end must be exactly one year minus one day after the start date"
            );
        }
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is not configured");
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must use YYYY-MM-DD format");
        }
    }

    public record FinancialYear(
            LocalDate startDate,
            LocalDate endDate,
            String code
    ) {
    }
}