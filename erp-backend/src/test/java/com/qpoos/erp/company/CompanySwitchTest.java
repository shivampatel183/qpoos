package com.qpoos.erp.company;

import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanySwitchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void switchCompanyWorksForNewCompanyOnV1Route() throws Exception {
        UserEntity user = userRepository.save(UserEntity.builder()
                .email("company-switch-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
        CompanyEntity company = companyRepository.save(CompanyEntity.builder()
                .userId(user.getId())
                .name("Switch Company")
                .isActive(true)
                .build());

        mockMvc.perform(post("/api/company/switch-company/" + company.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtService.createAccessToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(nullValue())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900));
    }

    @Test
    void switchCompanyWorksForNewCompanyOnLegacyRoute() throws Exception {
        UserEntity user = userRepository.save(UserEntity.builder()
                .email("company-switch-legacy-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
        CompanyEntity company = companyRepository.save(CompanyEntity.builder()
                .userId(user.getId())
                .name("Switch Company Legacy")
                .isActive(true)
                .build());

        mockMvc.perform(post("/api/company/switch-company/" + company.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtService.createAccessToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(nullValue())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
