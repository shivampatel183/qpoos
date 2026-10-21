package com.qpoos.erp.company;

import com.qpoos.erp.accounting.setup.AccountingBootstrapService;
import com.qpoos.erp.auth.config.AuthProperties;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.company.dto.CompanyRequest;
import com.qpoos.erp.company.dto.CompanyResponse;
import com.qpoos.erp.company.dto.CompanySummary;
import com.qpoos.erp.company.dto.CompanyAuthResponse;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.company.application.CompanyService;
import com.qpoos.erp.user.infrastructure.UserRepository;
import com.qpoos.erp.user.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    private final List<CompanyEntity> companies = new ArrayList<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private CompanyService companyService;
    private JwtService jwtService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.setAccessTokenMinutes(27);
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        companyService = new CompanyService(
                mockCompanyRepository(),
                passwordEncoder,
                jwtService,
                userRepository,
                mock(AccountingBootstrapService.class),
                properties
        );
    }

    @Test
    void switchCompanyUsesConfiguredAccessTokenExpiration() {
        UUID userId = UUID.randomUUID();
        CompanyEntity company = saveCompany(userId, "Configured Expiry Company", true);
        UserEntity user = UserEntity.builder().id(userId).email("user@example.com").build();
        doReturn(user).when(userRepository).getById(userId);
        when(jwtService.createAccessToken(user, company.getId())).thenReturn("company-token");

        CompanyAuthResponse response = companyService.switchCompany(company.getId(), userId);

        assertThat(response.accessToken()).isEqualTo("company-token");
        assertThat(response.expiresInSeconds()).isEqualTo(27 * 60);
    }

    @Test
    void createStoresCompanyForAuthenticatedUserOnly() {
        UUID userId = UUID.randomUUID();

        CompanyResponse response = companyService.create(userId, request("Acme Private Limited"));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Acme Private Limited");
        assertThat(companies).singleElement().satisfies(company -> {
            assertThat(company.getUserId()).isEqualTo(userId);
            assertThat(company.getName()).isEqualTo("Acme Private Limited");
            assertThat(company.getIsActive()).isTrue();
        });
    }

    @Test
    void listReturnsOnlyActiveCompaniesForAuthenticatedUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        saveCompany(ownerId, "Owner Active", true);
        saveCompany(ownerId, "Owner Inactive", false);
        saveCompany(otherUserId, "Other Active", true);

        List<CompanySummary> responses = companyService.list(ownerId);

        assertThat(responses)
                .extracting(CompanySummary::name)
                .containsExactly("Owner Active");
    }

    @Test
    void getRejectsCompanyOwnedByAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        CompanyEntity company = saveCompany(otherUserId, "Other Active", true);

        assertThatThrownBy(() -> companyService.get(ownerId, company.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void updateChangesOnlyAuthenticatedUsersCompany() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        CompanyEntity owned = saveCompany(ownerId, "Old Name", true);
        CompanyEntity other = saveCompany(otherUserId, "Other Name", true);

        CompanyResponse response = companyService.update(ownerId, owned.getId(), request("New Name"));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(owned.getName()).isEqualTo("New Name");
        assertThat(other.getName()).isEqualTo("Other Name");
        assertThatThrownBy(() -> companyService.update(ownerId, other.getId(), request("Blocked Name")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void deleteDeactivatesOnlyAuthenticatedUsersCompany() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        CompanyEntity owned = saveCompany(ownerId, "Owner Active", true);
        CompanyEntity other = saveCompany(otherUserId, "Other Active", true);

        companyService.delete(ownerId, owned.getId());

        assertThat(owned.getIsActive()).isFalse();
        assertThat(other.getIsActive()).isTrue();
        assertThatThrownBy(() -> companyService.delete(ownerId, other.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found");
    }

    private CompanyRequest request(String name) {
        return new CompanyRequest(
                name,
                "https://example.com/logo.png",
                false,
                null,
                "2026-04-01",
                "2027-03-31",
                "Line 1",
                "Line 2",
                "Mumbai",
                "Maharashtra",
                "India",
                "400001",
                "9999999999",
                "company@example.com",
                "https://example.com"
        );
    }

    private CompanyEntity saveCompany(UUID userId, String name, boolean active) {
        CompanyEntity company = CompanyEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(name)
                .isActive(active)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        companies.add(company);
        return company;
    }

    private CompanyRepository mockCompanyRepository() {
        CompanyRepository repository = mock(CompanyRepository.class);
        when(repository.save(any(CompanyEntity.class))).thenAnswer(invocation -> {
            CompanyEntity company = invocation.getArgument(0);
            if (company.getId() == null) {
                company.setId(UUID.randomUUID());
                company.setCreatedAt(OffsetDateTime.now());
            }
            company.setUpdatedAt(OffsetDateTime.now());
            companies.removeIf(existing -> existing.getId().equals(company.getId()));
            companies.add(company);
            return company;
        });
        when(repository.findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(any())).thenAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            return companies.stream()
                    .filter(company -> company.getUserId().equals(userId))
                    .filter(company -> Boolean.TRUE.equals(company.getIsActive()))
                    .toList();
        });
        when(repository.findAllByIsActiveTrue()).thenAnswer(invocation -> companies.stream()
                .filter(company -> Boolean.TRUE.equals(company.getIsActive()))
                .toList());
        when(repository.findByIdAndUserIdAndIsActiveTrue(any(), any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            UUID userId = invocation.getArgument(1);
            return companies.stream()
                    .filter(company -> company.getId().equals(id))
                    .filter(company -> company.getUserId().equals(userId))
                    .filter(company -> Boolean.TRUE.equals(company.getIsActive()))
                    .findFirst();
        });
        when(repository.existsByUserIdAndNameIgnoreCaseAndIsActiveTrue(any(), any())).thenAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            String name = invocation.getArgument(1);
            return companies.stream()
                    .filter(company -> company.getUserId().equals(userId))
                    .filter(company -> company.getName().equalsIgnoreCase(name))
                    .filter(company -> Boolean.TRUE.equals(company.getIsActive()))
                    .findAny()
                    .isPresent();
        });
        when(repository.existsByUserIdAndNameIgnoreCaseAndIdNotAndIsActiveTrue(any(), any(), any())).thenAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            String name = invocation.getArgument(1);
            UUID id = invocation.getArgument(2);
            return companies.stream()
                    .filter(company -> company.getUserId().equals(userId))
                    .filter(company -> company.getName().equalsIgnoreCase(name))
                    .filter(company -> !company.getId().equals(id))
                    .filter(company -> Boolean.TRUE.equals(company.getIsActive()))
                    .findAny()
                    .isPresent();
        });
        return repository;
    }
}
