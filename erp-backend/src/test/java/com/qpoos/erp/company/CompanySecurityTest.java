package com.qpoos.erp.company;

import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import com.qpoos.erp.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void companyListRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/company"))
                .andExpect(status().isForbidden());
    }

    @Test
    void companyCreateRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Token Required Private Limited"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void companyListAllowsValidToken() throws Exception {
        UserEntity user = userRepository.save(UserEntity.builder()
                .email("company-security-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());

        mockMvc.perform(get("/api/company")
                        .header("Authorization", "Bearer " + jwtService.createAccessToken(user)))
                .andExpect(status().isOk());
    }
}
