package com.qpoos.erp.accounting;

import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.company.application.CompanyService;
import com.qpoos.erp.company.dto.CompanyRequest;
import com.qpoos.erp.company.dto.CompanyResponse;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountingEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void accountGroupsGetUsesCompanyFromAccessToken() throws Exception {
        UserEntity user = userRepository.save(UserEntity.builder()
                .email("accounting-get-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
        CompanyResponse company = companyService.create(user.getId(), companyRequest("Accounting GET Company"));

        mockMvc.perform(get("/api/accounting/account-groups")
                        .header("Authorization", "Bearer " + jwtService.createAccessToken(user, company.id())))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAdminCanCreateGlobalAccountTypes() throws Exception {
        UserEntity user = createUser("user");
        UserEntity admin = createUser("admin");

        String payload = """
                {
                  "accountGroupCode": "ASSET",
                  "name": "Admin Managed Type",
                  "defaultLedgerType": "GENERAL",
                  "displayOrder": 500
                }
                """;

        mockMvc.perform(post("/api/accounting/account-types")
                        .header("Authorization", "Bearer " + jwtService.createAccessToken(user))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/accounting/account-types")
                        .header("Authorization", "Bearer " + jwtService.createAccessToken(admin))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private UserEntity createUser(String role) {
        return userRepository.save(UserEntity.builder()
                .email("accounting-" + role + "-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role(role)
                .isActive(true)
                .emailVerified(true)
                .build());
    }

    private CompanyRequest companyRequest(String name) {
        return new CompanyRequest(
                name,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
