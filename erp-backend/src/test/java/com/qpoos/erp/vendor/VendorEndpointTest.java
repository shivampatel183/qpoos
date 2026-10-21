package com.qpoos.erp.vendor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VendorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompanyService companyService;

    @Test
    void performsCrudAndKeepsVendorsIsolatedByTokenCompany() throws Exception {
        UserEntity user = createUser();
        CompanyResponse companyA = companyService.create(user.getId(), companyRequest("Vendor Company A"));
        CompanyResponse companyB = companyService.create(user.getId(), companyRequest("Vendor Company B"));
        String companyAToken = jwtService.createAccessToken(user, companyA.id());
        String companyBToken = jwtService.createAccessToken(user, companyB.id());

        MvcResult createResult = mockMvc.perform(post("/api/vendors")
                        .header("Authorization", bearer(companyAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyId").value(companyA.id().toString()))
                .andExpect(jsonPath("$.companyName").value("Acme Supplies"))
                .andExpect(jsonPath("$.displayName").value("Acme"))
                .andExpect(jsonPath("$.gstNo").value("27ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.email").value("accounts@acme.example"))
                .andExpect(jsonPath("$.ifscCode").value("HDFC0001234"))
                .andExpect(jsonPath("$.openingBalance").value(1500.75))
                .andReturn();

        JsonNode createdVendor = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long vendorId = createdVendor.get("id").asLong();

        mockMvc.perform(get("/api/vendors")
                        .header("Authorization", bearer(companyAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(vendorId));

        mockMvc.perform(get("/api/vendors")
                        .header("Authorization", bearer(companyBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", bearer(companyAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload().replace("\" Acme \"", "\"acme\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vendor display name already exists"));

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", bearer(companyBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Acme"));

        mockMvc.perform(get("/api/vendors/{vendorId}", vendorId)
                        .header("Authorization", bearer(companyBToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vendor not found"));

        String secondVendorPayload = createPayload()
                .replace("\" Acme Supplies \"", "\"Beta Supplies\"")
                .replace("\" Acme \"", "\"Beta\"");
        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", bearer(companyAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondVendorPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Beta"));

        mockMvc.perform(put("/api/vendors/{vendorId}", vendorId)
                        .header("Authorization", bearer(companyAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload().replace(
                                "\"Acme Industrial\"",
                                "\"beta\""
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vendor display name already exists"));

        mockMvc.perform(put("/api/vendors/{vendorId}", vendorId)
                        .header("Authorization", bearer(companyAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Industrial Supplies"))
                .andExpect(jsonPath("$.displayName").value("Acme Industrial"))
                .andExpect(jsonPath("$.mobileNo").value("9999999999"))
                .andExpect(jsonPath("$.openingBalance").value(2500.00));

        mockMvc.perform(delete("/api/vendors/{vendorId}", vendorId)
                        .header("Authorization", bearer(companyAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vendor deleted successfully."));

        mockMvc.perform(get("/api/vendors/{vendorId}", vendorId)
                        .header("Authorization", bearer(companyAToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vendor not found"));
    }

    @Test
    void requiresSelectedCompanyAndValidVendorData() throws Exception {
        UserEntity user = createUser();
        CompanyResponse company = companyService.create(user.getId(), companyRequest("Vendor Validation Company"));

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", bearer(jwtService.createAccessToken(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("A company must be selected"));

        String invalidPayload = """
                {
                  "companyName": "Invalid Vendor",
                  "firstName": "Invalid",
                  "lastName": "Vendor",
                  "displayName": "Invalid Vendor",
                  "email": "not-an-email",
                  "ifscCode": "INVALID",
                  "gstNo": "INVALID",
                  "openingBalance": -1
                }
                """;

        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", bearer(jwtService.createAccessToken(user, company.id())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    private UserEntity createUser() {
        return userRepository.save(UserEntity.builder()
                .email("vendor-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
    }

    private CompanyRequest companyRequest(String prefix) {
        return new CompanyRequest(
                prefix + " " + UUID.randomUUID(),
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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String createPayload() {
        return """
                {
                  "companyName": " Acme Supplies ",
                  "firstName": "Anita",
                  "lastName": "Shah",
                  "displayName": " Acme ",
                  "gstNo": "27abcde1234f1z5",
                  "email": "Accounts@Acme.Example",
                  "mobileNo": "9876543210",
                  "streetAddress1": "12 Market Road",
                  "streetAddress2": "Second Floor",
                  "city": "Mumbai",
                  "state": "Maharashtra",
                  "country": "India",
                  "pinCode": "400001",
                  "notes": "Priority vendor",
                  "accountHolderName": "Acme Supplies Pvt Ltd",
                  "accountNumber": "1234567890",
                  "ifscCode": "hdfc0001234",
                  "openingBalance": 1500.75
                }
                """;
    }

    private String updatePayload() {
        return """
                {
                  "companyName": "Acme Industrial Supplies",
                  "firstName": "Anita",
                  "lastName": "Shah",
                  "displayName": "Acme Industrial",
                  "gstNo": "27ABCDE1234F1Z5",
                  "email": "accounts@acme.example",
                  "mobileNo": "9999999999",
                  "streetAddress1": "12 Market Road",
                  "city": "Mumbai",
                  "state": "Maharashtra",
                  "country": "India",
                  "pinCode": "400001",
                  "accountHolderName": "Acme Industrial Supplies Pvt Ltd",
                  "accountNumber": "1234567890",
                  "ifscCode": "HDFC0001234",
                  "openingBalance": 2500.00
                }
                """;
    }
}
