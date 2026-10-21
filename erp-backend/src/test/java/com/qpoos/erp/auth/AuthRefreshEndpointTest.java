package com.qpoos.erp.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.user.domain.UserEntity;
import com.qpoos.erp.user.infrastructure.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void refreshWithoutBodyReturnsCompanyLessAccessToken() throws Exception {
        LoginSession session = login(createUser());

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = accessToken(result);
        assertThat(jwtService.validateAndGetClaims(accessToken).get("companyId")).isNull();
    }

    @Test
    void refreshWithOwnedCompanyReturnsScopedTokenThatCanCallAccounting() throws Exception {
        UserEntity user = createUser();
        CompanyEntity company = createCompany(user.getId(), true);
        LoginSession session = login(user);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":\"" + company.getId() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = accessToken(result);
        assertThat(jwtService.validateAndGetClaims(accessToken).get("companyId", String.class))
                .isEqualTo(company.getId().toString());

        mockMvc.perform(get("/api/accounting/account-groups")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void unauthorizedCompanyReturnsForbiddenWithoutRevokingRefreshToken() throws Exception {
        UserEntity user = createUser();
        CompanyEntity otherCompany = createCompany(UUID.randomUUID(), true);
        LoginSession session = login(user);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":\"" + otherCompany.getId() + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void malformedCompanyIdReturnsBadRequest() throws Exception {
        LoginSession session = login(createUser());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(session.refreshCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest());
    }

    private UserEntity createUser() {
        return userRepository.save(UserEntity.builder()
                .email("refresh-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
    }

    private CompanyEntity createCompany(UUID userId, boolean active) {
        return companyRepository.save(CompanyEntity.builder()
                .userId(userId)
                .name("Refresh Company " + UUID.randomUUID())
                .isActive(active)
                .build());
    }

    private LoginSession login(UserEntity user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestBody(
                                user.getEmail(),
                                "StrongPass123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return new LoginSession(cookie);
    }

    private String accessToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("accessToken").asText();
    }

    private record LoginRequestBody(String email, String password) {
    }

    private record LoginSession(Cookie refreshCookie) {
    }
}
