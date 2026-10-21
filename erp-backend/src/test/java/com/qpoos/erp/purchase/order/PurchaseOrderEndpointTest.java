package com.qpoos.erp.purchase.order;

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
class PurchaseOrderEndpointTest {

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
    void createsCalculatesAndTransitionsPurchaseOrder() throws Exception {
        UserEntity user = createUser();
        CompanyResponse company = companyService.create(user.getId(), companyRequest("Purchase Order Company"));
        CompanyEntity companyEntity = companyServiceEntity(company.id());
        VendorEntity vendor = vendorRepository.save(VendorEntity.builder()
                .company(companyEntity).displayName("Acme Supplies").active(true).build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .company(companyEntity).name("Keyboard").type(ProductType.INVENTORY)
                .purchaseCost(new BigDecimal("100.00")).active(true).trackInventory(true).build());
        String token = jwtService.createAccessToken(user, company.id());

        MvcResult createResult = mockMvc.perform(post("/api/purchase-orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(vendor.getId(), product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("PO-2026-27-00001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.taxAmount").value(36.00))
                .andExpect(jsonPath("$.totalAmount").value(236.00))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andReturn();
        long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/purchase-orders/{id}/submit", orderId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        mockMvc.perform(put("/api/purchase-orders/{id}", orderId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(vendor.getId(), product.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only draft purchase orders can be edited"));
    }

    @Test
    void keepsOrdersIsolatedByCompany() throws Exception {
        UserEntity user = createUser();
        CompanyResponse companyA = companyService.create(user.getId(), companyRequest("Order Company A"));
        CompanyResponse companyB = companyService.create(user.getId(), companyRequest("Order Company B"));
        String tokenA = jwtService.createAccessToken(user, companyA.id());
        String tokenB = jwtService.createAccessToken(user, companyB.id());

        mockMvc.perform(get("/api/purchase-orders").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/purchase-orders").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private CompanyEntity companyServiceEntity(UUID id) {
                return companyRepository.findById(id).orElseThrow();
    }

    private UserEntity createUser() {
        return userRepository.save(UserEntity.builder()
                .email("purchase-order-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user").isActive(true).emailVerified(true).build());
    }

    private CompanyRequest companyRequest(String prefix) {
        return new CompanyRequest(prefix + " " + UUID.randomUUID(), null, false, null,
                "2026-04-01", "2027-03-31", null, null, null, null, null, null, null,
                null, null);
    }

    private String orderPayload(Long vendorId, Long productId) {
        return """
                {
                  "orderDate": "2026-09-20",
                  "vendorId": %d,
                  "deliveryTerm": "FOB",
                  "expectedDeliveryDate": "2026-10-05",
                  "deliveryAddress": {
                    "line1": "Warehouse 1",
                    "city": "Pune",
                    "state": "Maharashtra",
                    "country": "India",
                    "postalCode": "411001"
                  },
                  "roundOff": 0,
                  "notes": "Test order",
                  "lines": [
                    {
                      "lineNo": 1,
                      "productId": %d,
                      "description": "USB keyboard",
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