package com.qpoos.erp.accounting;

import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.accountcatalog.infrastructure.AccountTypeDefinitionRepository;
import com.qpoos.erp.accounting.accountgroup.infrastructure.AccountGroupRepository;
import com.qpoos.erp.accounting.accounttype.infrastructure.AccountTypeRepository;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.setup.AccountingBootstrapService;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import com.qpoos.erp.company.application.CompanyService;
import com.qpoos.erp.company.dto.CompanyRequest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AccountingBootstrapServiceTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    @Autowired
    private AccountTypeDefinitionRepository definitionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private AccountingBootstrapService accountingBootstrapService;

    @Test
    void creatingCompanyInstallsDefaultChartOfAccounts() {
        UUID userId = UUID.randomUUID();
        UUID companyId = companyService.create(userId, request("Default COA " + UUID.randomUUID())).id();

        assertThat(accountTypeRepository.findAll()).hasSize(5);
        assertThat(definitionRepository.findAll()).hasSize(18);
        assertThat(accountGroupRepository.findAllByCompany_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(companyId))
                .extracting("code")
                .contains(
                        "CASH_EQUIVALENTS",
                        "BANK",
                        "ACCOUNTS_RECEIVABLE",
                        "GST_RECEIVABLE",
                        "GST_PAYABLE",
                        "COST_OF_GOODS_SOLD"
                );
        assertThat(ledgerRepository.findAllByCompany_IdAndActiveTrueOrderByNameAsc(companyId))
                .extracting("code")
                .contains(
                        "CASH_IN_HAND",
                        "BANK_ACCOUNT",
                        "CUSTOMER_RECEIVABLE",
                        "SUPPLIER_PAYABLE",
                        "OUTPUT_GST_PAYABLE",
                        "DOMESTIC_SALES",
                        "PURCHASE_ACCOUNT",
                        "RENT_EXPENSE",
                        "CAPITAL_ACCOUNT"
                );
        assertThat(ledgerRepository.findAllByCompany_IdAndActiveTrueOrderByNameAsc(companyId))
                .allSatisfy(ledger -> {
                    assertThat(ledger.getOpeningBalance()).isZero();
                    assertThat(ledger.getOpeningBalanceAsOfDate()).isEqualTo(LocalDate.of(2026, 4, 1));
                    assertThat(ledger.getAccountGroup().getAccountTypeDefinition()).isNotNull();
                });
    }

    @Test
    void defaultChartInstallerIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID companyId = companyService.create(userId, request("Idempotent COA " + UUID.randomUUID())).id();
        CompanyEntity company = companyRepository.findById(companyId).orElseThrow();
        int groupCount = accountGroupRepository.findAllByCompany_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(companyId).size();
        int ledgerCount = ledgerRepository.findAllByCompany_IdAndActiveTrueOrderByNameAsc(companyId).size();

        accountingBootstrapService.ensureDefaultChartForCompany(company);

        assertThat(accountGroupRepository.findAllByCompany_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(companyId)).hasSize(groupCount);
        assertThat(ledgerRepository.findAllByCompany_IdAndActiveTrueOrderByNameAsc(companyId)).hasSize(ledgerCount);
    }

    private CompanyRequest request(String name) {
        return new CompanyRequest(
                name,
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
