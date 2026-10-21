package com.qpoos.erp.accounting;

import com.qpoos.erp.accounting.domain.NormalBalance;
import com.qpoos.erp.accounting.accountcatalog.infrastructure.AccountTypeDefinitionRepository;
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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private AccountTypeDefinitionRepository definitionRepository;

    @Test
    void createsAccountWithDerivedClassificationAndGeneratedCode() throws Exception {
        UserEntity user = userRepository.save(UserEntity.builder()
                .email("ledger-create-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
        CompanyResponse company = companyService.create(user.getId(), companyRequest());
        Long bankTypeId = definitionRepository.findByCodeIgnoreCase("BANK").orElseThrow().getId();
        String token = jwtService.createAccessToken(user, company.id());
        String payload = """
                {
                  "accountTypeId": %d,
                  "name": "Primary Operating Bank",
                  "openingBalance": 12500.50,
                  "openingBalanceAsOfDate": "2026-04-01",
                  "description": "Main operating account"
                }
                """.formatted(bankTypeId);

        mockMvc.perform(post("/api/accounting/ledgers")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", startsWith("PRIMARY_OPERATING_BANK")))
                .andExpect(jsonPath("$.accountGroupCode").value("ASSET"))
                .andExpect(jsonPath("$.accountTypeCode").value("BANK"))
                .andExpect(jsonPath("$.normalBalance").value("DEBIT"))
                .andExpect(jsonPath("$.openingBalance").value(12500.50))
                .andExpect(jsonPath("$.balance").value(12500.50))
                .andExpect(jsonPath("$.description").value("Main operating account"));
    }

    private CompanyRequest companyRequest() {
        return new CompanyRequest(
                "Ledger API " + UUID.randomUUID(),
                null,
                false,
                null,
                "2026-04-01",
                "2027-03-31",
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
