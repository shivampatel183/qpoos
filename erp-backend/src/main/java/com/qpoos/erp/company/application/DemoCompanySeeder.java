package com.qpoos.erp.company.application;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qpoos.erp.accounting.chartofaccount.setup.AccountingBootstrapService;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;

import lombok.RequiredArgsConstructor;

/** Creates a login-ready sample company for local development databases. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed-company", havingValue = "true")
public class DemoCompanySeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@qpoos.local";
    public static final String DEMO_PASSWORD = "Demo123!";
    private static final String DEMO_COMPANY_NAME = "Qpoos Demo Company";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountingBootstrapService accountingBootstrapService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserEntity user = userRepository.findByEmailIgnoreCase(DEMO_EMAIL).orElseGet(() ->
                userRepository.save(UserEntity.builder()
                        .email(DEMO_EMAIL)
                        .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                        .role("user")
                        .isActive(true)
                        .emailVerified(true)
                        .emailVerifiedAt(java.time.OffsetDateTime.now())
                        .build()));

        if (companyRepository.findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId()).stream()
                .anyMatch(company -> DEMO_COMPANY_NAME.equals(company.getName()))) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate financialYearStart = today.getMonthValue() >= 4
                ? LocalDate.of(today.getYear(), 4, 1)
                : LocalDate.of(today.getYear() - 1, 4, 1);
        CompanyEntity company = companyRepository.save(CompanyEntity.builder()
                .userId(user.getId())
                .name(DEMO_COMPANY_NAME)
                .financialYearStart(financialYearStart.toString())
                .financialYearEnd(financialYearStart.plusYears(1).minusDays(1).toString())
                .addressLine1("123 Demo Street")
                .city("Bengaluru")
                .state("Karnataka")
                .country("India")
                .pincode("560001")
                .phoneNumber("+91-0000000000")
                .email(DEMO_EMAIL)
                .website("https://example.com")
                .isActive(true)
                .build());

        accountingBootstrapService.ensureDefaultChartForCompany(company);
    }
}
