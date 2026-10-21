package com.qpoos.erp.company.application;

import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.accounting.setup.AccountingBootstrapService;
import com.qpoos.erp.auth.config.AuthProperties;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.company.dto.CompanyAuthResponse;
import com.qpoos.erp.company.dto.CompanyRequest;
import com.qpoos.erp.company.dto.CompanyResponse;
import com.qpoos.erp.company.dto.CompanySummary;
import com.qpoos.erp.user.infrastructure.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AccountingBootstrapService accountingBootstrapService;
    private final AuthProperties properties;

    @Transactional
    public CompanyResponse create(UUID userId, CompanyRequest request) {
        String name = normalizeName(request.name());
        if (companyRepository.existsByUserIdAndNameIgnoreCaseAndIsActiveTrue(userId, name)) {
            throw new IllegalArgumentException("Company name already exists");
        }

        CompanyEntity company = CompanyEntity.builder()
                .userId(userId)
                .name(name)
                .logo(blankToNull(request.logo()))
                .passwordOn(Boolean.TRUE.equals(request.passwordOn()))
                .passwordHash(passwordHash(request.passwordOn(), request.password(), null))
                .financialYearStart(blankToNull(request.financialYearStart()))
                .financialYearEnd(blankToNull(request.financialYearEnd()))
                .addressLine1(blankToNull(request.addressLine1()))
                .addressLine2(blankToNull(request.addressLine2()))
                .city(blankToNull(request.city()))
                .state(blankToNull(request.state()))
                .country(blankToNull(request.country()))
                .pincode(blankToNull(request.pincode()))
                .phoneNumber(blankToNull(request.phoneNumber()))
                .email(blankToNull(request.email()))
                .website(blankToNull(request.website()))
                .isActive(true)
                .build();

        CompanyEntity savedCompany = companyRepository.save(company);
        accountingBootstrapService.ensureDefaultChartForCompany(savedCompany);
        return toResponse(savedCompany);
    }

    public List<CompanySummary> list(UUID userId) {
        return companyRepository.findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId).stream()
                .map(c -> new CompanySummary(c.getId(), c.getName()))
                .toList();
    }

    public CompanyResponse get(UUID userId, UUID companyId) {
        return toResponse(getOwnedActiveCompany(userId, companyId));
    }

    @Transactional
    public CompanyResponse update(UUID userId, UUID companyId, CompanyRequest request) {
        CompanyEntity company = getOwnedActiveCompany(userId, companyId);
        String name = normalizeName(request.name());
        if (companyRepository.existsByUserIdAndNameIgnoreCaseAndIdNotAndIsActiveTrue(userId, name, companyId)) {
            throw new IllegalArgumentException("Company name already exists");
        }

        company.setName(name);
        company.setLogo(blankToNull(request.logo()));
        company.setPasswordOn(Boolean.TRUE.equals(request.passwordOn()));
        company.setPasswordHash(passwordHash(request.passwordOn(), request.password(), company.getPasswordHash()));
        company.setFinancialYearStart(blankToNull(request.financialYearStart()));
        company.setFinancialYearEnd(blankToNull(request.financialYearEnd()));
        company.setAddressLine1(blankToNull(request.addressLine1()));
        company.setAddressLine2(blankToNull(request.addressLine2()));
        company.setCity(blankToNull(request.city()));
        company.setState(blankToNull(request.state()));
        company.setCountry(blankToNull(request.country()));
        company.setPincode(blankToNull(request.pincode()));
        company.setPhoneNumber(blankToNull(request.phoneNumber()));
        company.setEmail(blankToNull(request.email()));
        company.setWebsite(blankToNull(request.website()));

        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public void delete(UUID userId, UUID companyId) {
        CompanyEntity company = getOwnedActiveCompany(userId, companyId);
        company.setIsActive(false);
        companyRepository.save(company);
    }

    @Transactional
    public CompanyAuthResponse switchCompany(UUID companyId, UUID userId){
        List<CompanySummary> userCompanies = list(userId);
        CompanySummary company = userCompanies.stream()
                .filter(c -> c.id().equals(companyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Company not found"));
        var user = userRepository.getById(userId);
        var token = jwtService.createAccessToken(user, companyId);
        return CompanyAuthResponse.bearer(token, properties.accessTokenSeconds());
    }

    private CompanyEntity getOwnedActiveCompany(UUID userId, UUID companyId) {
        return companyRepository.findByIdAndUserIdAndIsActiveTrue(companyId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    }

    private String passwordHash(Boolean passwordOn, String password, String existingHash) {
        if (!Boolean.TRUE.equals(passwordOn)) {
            return null;
        }
        if (password == null || password.isBlank()) {
            if (existingHash != null && !existingHash.isBlank()) {
                return existingHash;
            }
            throw new IllegalArgumentException("Company password is required when password is enabled");
        }
        return passwordEncoder.encode(password);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CompanyResponse toResponse(CompanyEntity company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getLogo(),
                company.isPasswordOn(),
                company.getFinancialYearStart(),
                company.getFinancialYearEnd(),
                company.getAddressLine1(),
                company.getAddressLine2(),
                company.getCity(),
                company.getState(),
                company.getCountry(),
                company.getPincode(),
                company.getPhoneNumber(),
                company.getEmail(),
                company.getWebsite(),
                company.getIsActive(),
                company.getLastLoginAt(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
