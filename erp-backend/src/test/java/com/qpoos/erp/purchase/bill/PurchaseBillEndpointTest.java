package com.qpoos.erp.purchase.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.company.application.CompanyService;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.dto.CompanyRequest;
import com.qpoos.erp.company.dto.CompanyResponse;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.product.domain.ProductEntity;
import com.qpoos.erp.product.domain.ProductType;
import com.qpoos.erp.product.infrastructure.ProductRepository;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import com.qpoos.erp.vendor.domain.VendorEntity;
import com.qpoos.erp.vendor.infrastructure.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseBillEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CompanyService companyService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private ProductRepository productRepository;

    @Test
    void createsPostsAndKeepsPurchaseBillsIsolatedByCompany() throws Exception {
        UserEntity user = createUser();
        CompanyResponse companyA = companyService.create(user.getId(), companyRequest("Bill Company A"));
        CompanyResponse companyB = companyService.create(user.getId(), companyRequest("Bill Company B"));
        CompanyEntity companyEntityA = companyServiceEntity(companyA.id());
        CompanyEntity companyEntityB = companyServiceEntity(companyB.id());

        VendorEntity vendorA = vendorRepository.save(VendorEntity.builder()
                .company(companyEntityA).displayName("Alpha Supplies").active(true).build());
        VendorEntity vendorB = vendorRepository.save(VendorEntity.builder()
                .company(companyEntityB).displayName("Beta Supplies").active(true).build());

        ProductEntity productA = productRepository.save(ProductEntity.builder()
                .company(companyEntityA).name("Monitor").type(ProductType.INVENTORY)
                .purchaseCost(new BigDecimal("100.00")).active(true).trackInventory(true).build());
        ProductEntity productB = productRepository.save(ProductEntity.builder()
                .company(companyEntityB).name("Display").type(ProductType.INVENTORY)
                .purchaseCost(new BigDecimal("120.00")).active(true).trackInventory(true).build());

        String tokenA = jwtService.createAccessToken(user, companyA.id());
        String tokenB = jwtService.createAccessToken(user, companyB.id());

        MvcResult createResult = mockMvc.perform(post("/api/purchase-bills")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billPayload(vendorA.getId(), productA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billNumber").value("BILL-2026-27-00001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.taxAmount").value(36.00))
                .andExpect(jsonPath("$.totalAmount").value(236.00))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andReturn();
        long billId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/purchase-bills/{id}/post", billId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.postedVoucherId").value(billId));

        mockMvc.perform(put("/api/purchase-bills/{id}", billId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billPayload(vendorA.getId(), productA.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only draft purchase bills can be edited"));

        mockMvc.perform(get("/api/purchase-bills").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].billNumber").value("BILL-2026-27-00001"));

        mockMvc.perform(get("/api/purchase-bills").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/purchase-bills/{id}/post", billId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));

        mockMvc.perform(post("/api/purchase-bills/{id}/cancel", billId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid document status transition: POSTED -> CANCELLED"));

        mockMvc.perform(post("/api/purchase-bills")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billPayload(vendorA.getId(), productA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billNumber").value("BILL-2026-27-00002"));

        mockMvc.perform(post("/api/purchase-bills")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billPayload(vendorB.getId(), productB.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billNumber").value("BILL-2026-27-00001"));
    }

    private CompanyEntity companyServiceEntity(UUID id) {
        return companyRepository.findById(id).orElseThrow();
    }

    private UserEntity createUser() {
        return userRepository.save(UserEntity.builder()
                .email("purchase-bill-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user").isActive(true).emailVerified(true).build());
    }

    private CompanyRequest companyRequest(String prefix) {
        return new CompanyRequest(prefix + " " + UUID.randomUUID(), null, false, null,
                "2026-04-01", "2027-03-31", null, null, null, null, null, null, null,
                null, null);
    }

    private String billPayload(Long vendorId, Long productId) {
        return """
                {
                  "billDate": "2026-09-20",
                  "vendorId": %d,
                  "paymentTermId": null,
                  "dueDate": "2026-10-05",
                  "roundOff": 0,
                  "notes": "Test bill",
                  "lines": [
                    {
                      "lineNo": 1,
                      "productId": %d,
                      "description": "USB monitor",
                      "quantity": 2,
                      "unit": "PCS",
                      "rate": 100,
                      "discount": 0,
                      "taxRate": 18
                    }
                  ]
                }
                """.formatted(vendorId, productId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
