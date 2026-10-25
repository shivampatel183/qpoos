package com.qpoos.erp.purchase.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.qpoos.erp.accounting.chartofaccount.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.chartofaccount.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.common.security.JwtService;
import com.qpoos.erp.company.application.CompanyService;
import com.qpoos.erp.company.dto.CompanyRequest;
import com.qpoos.erp.company.dto.CompanyResponse;
import com.qpoos.erp.purchase.expense.domain.TransactionEntity;
import com.qpoos.erp.purchase.expense.infrastructure.TransactionRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExpensePostingEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CompanyService companyService;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private TransactionRepository transactionRepository;

    @Test
    void savesAndPostsPaidExpenseThroughCentralJournalEngineOnlyOnce() throws Exception {
        UserEntity user = userRepository.save(UserEntity.builder()
                .email("expense-post-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!"))
                .role("user")
                .isActive(true)
                .emailVerified(true)
                .build());
        CompanyResponse company = companyService.create(user.getId(), companyRequest());
        String token = jwtService.createAccessToken(user, company.id());
        LedgerEntity rentExpense = ledgerRepository
                .findByCompany_IdAndCodeIgnoreCase(company.id(), "RENT_EXPENSE")
                .orElseThrow();
        LedgerEntity bankAccount = ledgerRepository
                .findByCompany_IdAndCodeIgnoreCase(company.id(), "BANK_ACCOUNT")
                .orElseThrow();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload(rentExpense.getId(), bankAccount.getId())
                                .replace("\"paymentLedgerId\": " + bankAccount.getId(), "\"paymentLedgerId\": null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Paid from account is required for an expense"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload(rentExpense.getId(), bankAccount.getId())
                                .replace("\"paymentDate\": \"2026-08-01\"", "\"paymentDate\": null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment date is required for an expense"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload(rentExpense.getId(), bankAccount.getId())
                                .replace("\"totalAmount\": 118.00", "\"totalAmount\": 119.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Expense total does not match its calculated source lines"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        MvcResult created = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload(rentExpense.getId(), bankAccount.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.journalEntryId").isNumber())
                .andExpect(jsonPath("$.paymentLedgerId").value(bankAccount.getId()))
                .andReturn();
        long transactionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
        long journalEntryId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("journalEntryId").asLong();

        MvcResult firstPost = mockMvc.perform(post("/api/transactions/{id}/post", transactionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalEntryId").isNumber())
                .andReturn();
        assertEquals(
                journalEntryId,
                objectMapper.readTree(firstPost.getResponse().getContentAsString())
                        .get("journalEntryId").asLong()
        );

        mockMvc.perform(post("/api/transactions/{id}/post", transactionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalEntryId").value(journalEntryId));

        MvcResult journalResult = mockMvc.perform(get("/api/accounting/journals/{id}", journalEntryId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("EXPENSE"))
                .andExpect(jsonPath("$.sourceDocumentId").value(transactionId))
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andReturn();
        JsonNode journalLines = objectMapper.readTree(journalResult.getResponse().getContentAsString())
                .get("lines");
        assertJournalAmount(journalLines, "RENT_EXPENSE", "debitAmount", "100.00");
        assertJournalAmount(journalLines, "GST_INPUT_CREDIT", "debitAmount", "18.00");
        assertJournalAmount(journalLines, "BANK_ACCOUNT", "creditAmount", "118.00");

        mockMvc.perform(get("/api/accounting/journals")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload(rentExpense.getId(), bankAccount.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Posted transactions cannot be edited"));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Posted transactions cannot be deleted"));

        TransactionEntity transaction = transactionRepository.findByIdAndCompany_IdAndActiveTrue(
                transactionId,
                company.id()
        ).orElseThrow();
        assertEquals(journalEntryId, transaction.getJournalEntry().getId());
    }

    private String expensePayload(Long accountId, Long paymentLedgerId) {
        return """
                {
                  "transactionDate": "2026-08-01",
                  "transactionType": "EXPENSE",
                  "subtotal": 100.00,
                  "discountAmount": 0.00,
                  "taxAmount": 18.00,
                  "roundOff": 0.00,
                  "totalAmount": 118.00,
                  "paymentDate": "2026-08-01",
                  "paymentLedgerId": %d,
                  "paymentStatus": "PAID",
                  "lines": [
                    {
                      "lineNo": 1,
                      "lineType": "ACCOUNT",
                      "accountId": %d,
                      "quantity": 1.000,
                      "rate": 100.00,
                      "discount": 0.00,
                      "taxRate": 18.00,
                      "taxAmount": 18.00,
                      "amount": 118.00
                    }
                  ]
                }
                """.formatted(paymentLedgerId, accountId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertJournalAmount(JsonNode lines, String ledgerCode, String side, String expected) {
        for (JsonNode line : lines) {
            if (ledgerCode.equals(line.get("ledgerCode").asText())) {
                assertEquals(
                        0,
                        new java.math.BigDecimal(expected).compareTo(line.get(side).decimalValue())
                );
                return;
            }
        }
        throw new AssertionError("Journal line not found for ledger " + ledgerCode);
    }

    private CompanyRequest companyRequest() {
        return new CompanyRequest(
                "Expense Posting " + UUID.randomUUID(),
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
