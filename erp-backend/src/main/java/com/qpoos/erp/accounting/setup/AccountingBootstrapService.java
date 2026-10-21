package com.qpoos.erp.accounting.setup;

import com.qpoos.erp.accounting.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.domain.LedgerType;
import com.qpoos.erp.accounting.domain.NormalBalance;
import com.qpoos.erp.accounting.domain.StatementType;
import com.qpoos.erp.accounting.accountcatalog.domain.AccountTypeDefinitionEntity;
import com.qpoos.erp.accounting.accountcatalog.infrastructure.AccountTypeDefinitionRepository;
import com.qpoos.erp.accounting.accountgroup.domain.AccountGroupEntity;
import com.qpoos.erp.accounting.accountgroup.infrastructure.AccountGroupRepository;
import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import com.qpoos.erp.accounting.accounttype.infrastructure.AccountTypeRepository;
import com.qpoos.erp.accounting.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.company.infrastructure.CompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingBootstrapService implements ApplicationRunner {

    private static final Map<String, String> LEGACY_GROUP_MAPPING = Map.ofEntries(
            Map.entry("CURRENT_ASSETS", "OTHER_CURRENT_ASSETS"),
            Map.entry("CASH", "CASH_EQUIVALENTS"),
            Map.entry("BANK_ACCOUNTS", "BANK"),
            Map.entry("ACCOUNTS_RECEIVABLE", "ACCOUNTS_RECEIVABLE"),
            Map.entry("CURRENT_LIABILITIES", "OTHER_CURRENT_LIABILITIES"),
            Map.entry("ACCOUNTS_PAYABLE", "ACCOUNTS_PAYABLE"),
            Map.entry("GST_PAYABLE", "GST_PAYABLE"),
            Map.entry("CAPITAL", "EQUITY"),
            Map.entry("OWNER_CAPITAL", "EQUITY"),
            Map.entry("SALES", "INCOME"),
            Map.entry("DOMESTIC_SALES", "INCOME"),
            Map.entry("PURCHASES", "COST_OF_GOODS_SOLD"),
            Map.entry("PURCHASE_ACCOUNTS", "COST_OF_GOODS_SOLD"),
            Map.entry("INDIRECT_EXPENSES", "EXPENSES"),
            Map.entry("RENT", "EXPENSES")
    );

    private final AccountTypeRepository accountTypeRepository;
    private final AccountTypeDefinitionRepository definitionRepository;
    private final AccountGroupRepository accountGroupRepository;
    private final LedgerRepository ledgerRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAccountGroups();
        seedAccountTypeDefinitions();
        companyRepository.findAllByIsActiveTrue().forEach(this::ensureDefaultChartForCompany);
    }

    @Transactional
    public void ensureDefaultChartForCompany(CompanyEntity company) {
        seedAccountGroups();
        seedAccountTypeDefinitions();
        Map<String, AccountTypeDefinitionEntity> definitions = definitionRepository.findAll().stream()
                .collect(Collectors.toMap(AccountTypeDefinitionEntity::getCode, Function.identity()));

        migrateLegacyGroups(company, definitions);
        definitions.values().forEach(definition -> ensureCompanyGroup(company, definition));

        LocalDate openingDate = resolveOpeningDate(company);
        ensureLedger(company, definitions.get("CASH_EQUIVALENTS"), "CASH_IN_HAND", "Cash in Hand", openingDate);
        ensureLedger(company, definitions.get("BANK"), "BANK_ACCOUNT", "Bank Account", openingDate);
        ensureLedger(company, definitions.get("ACCOUNTS_RECEIVABLE"), "CUSTOMER_RECEIVABLE", "Customer Receivable", openingDate);
        ensureLedger(company, definitions.get("ACCOUNTS_PAYABLE"), "SUPPLIER_PAYABLE", "Supplier Payable", openingDate);
        ensureLedger(company, definitions.get("GST_PAYABLE"), "OUTPUT_GST_PAYABLE", "Output GST Payable", openingDate);
        ensureLedger(company, definitions.get("INCOME"), "DOMESTIC_SALES", "Domestic Sales", openingDate);
        ensureLedger(company, definitions.get("COST_OF_GOODS_SOLD"), "PURCHASE_ACCOUNT", "Purchase Account", openingDate);
        ensureLedger(company, definitions.get("EXPENSES"), "RENT_EXPENSE", "Rent Expense", openingDate);
        ensureLedger(company, definitions.get("EQUITY"), "CAPITAL_ACCOUNT", "Capital Account", openingDate);

        ledgerRepository.findAllByCompany_Id(company.getId()).forEach(ledger -> {
            if (ledger.getOpeningBalance() == null) {
                ledger.setOpeningBalance(BigDecimal.ZERO);
            }
            if (ledger.getOpeningBalanceAsOfDate() == null) {
                ledger.setOpeningBalanceAsOfDate(openingDate);
            }
            ledgerRepository.save(ledger);
        });
    }

    private void seedAccountGroups() {
        List<AccountGroupSeed> seeds = List.of(
                new AccountGroupSeed("ASSET", "Asset", NormalBalance.DEBIT, StatementType.BALANCE_SHEET, 10),
                new AccountGroupSeed("LIABILITY", "Liability", NormalBalance.CREDIT, StatementType.BALANCE_SHEET, 20),
                new AccountGroupSeed("EQUITY", "Equity", NormalBalance.CREDIT, StatementType.BALANCE_SHEET, 30),
                new AccountGroupSeed("INCOME", "Income", NormalBalance.CREDIT, StatementType.PROFIT_LOSS, 40),
                new AccountGroupSeed("EXPENSE", "Expense", NormalBalance.DEBIT, StatementType.PROFIT_LOSS, 50)
        );

        for (AccountGroupSeed seed : seeds) {
            AccountTypeEntity accountGroup = accountTypeRepository.findByCodeIgnoreCase(seed.code())
                    .orElseGet(AccountTypeEntity::new);
            accountGroup.setCode(seed.code());
            accountGroup.setName(seed.name());
            accountGroup.setNormalBalance(seed.normalBalance());
            accountGroup.setStatementType(seed.statementType());
            accountGroup.setDisplayOrder(seed.displayOrder());
            accountGroup.setSystemDefined(true);
            accountTypeRepository.save(accountGroup);
        }
    }

    private void seedAccountTypeDefinitions() {
        Map<String, AccountTypeEntity> groups = accountTypeRepository.findAll().stream()
                .collect(Collectors.toMap(AccountTypeEntity::getCode, Function.identity()));
        List<AccountTypeDefinitionSeed> seeds = List.of(
                seed("ASSET", "CASH_EQUIVALENTS", "Cash and Cash Equivalents", LedgerType.CASH, 10),
                seed("ASSET", "BANK", "Bank", LedgerType.BANK, 20),
                seed("ASSET", "ACCOUNTS_RECEIVABLE", "Accounts Receivable (A/R)", LedgerType.CUSTOMER, 30),
                seed("ASSET", "OTHER_CURRENT_ASSETS", "Other Current Assets", LedgerType.GENERAL, 40),
                seed("ASSET", "FIXED_ASSETS", "Fixed Assets", LedgerType.GENERAL, 50),
                seed("ASSET", "OTHER_ASSETS", "Other Assets", LedgerType.GENERAL, 60),
                seed("ASSET", "GST_RECEIVABLE", "GST Receivable", LedgerType.TAX, 70),
                seed("LIABILITY", "CREDIT_CARD", "Credit Card", LedgerType.GENERAL, 80),
                seed("LIABILITY", "ACCOUNTS_PAYABLE", "Accounts Payable (A/P)", LedgerType.SUPPLIER, 90),
                seed("LIABILITY", "OTHER_CURRENT_LIABILITIES", "Other Current Liabilities", LedgerType.GENERAL, 100),
                seed("LIABILITY", "LONG_TERM_LIABILITIES", "Long Term Liabilities", LedgerType.GENERAL, 110),
                seed("LIABILITY", "GST_PAYABLE", "GST Payable", LedgerType.TAX, 120),
                seed("EQUITY", "EQUITY", "Equity", LedgerType.GENERAL, 130),
                seed("INCOME", "INCOME", "Income", LedgerType.GENERAL, 140),
                seed("INCOME", "OTHER_INCOME", "Other Income", LedgerType.GENERAL, 150),
                seed("EXPENSE", "COST_OF_GOODS_SOLD", "Cost of Goods Sold", LedgerType.GENERAL, 160),
                seed("EXPENSE", "EXPENSES", "Expenses", LedgerType.GENERAL, 170),
                seed("EXPENSE", "OTHER_EXPENSE", "Other Expense", LedgerType.GENERAL, 180)
        );

        for (AccountTypeDefinitionSeed seed : seeds) {
            AccountTypeDefinitionEntity definition = definitionRepository.findByCodeIgnoreCase(seed.code())
                    .orElseGet(AccountTypeDefinitionEntity::new);
            definition.setAccountGroup(groups.get(seed.accountGroupCode()));
            definition.setCode(seed.code());
            definition.setName(seed.name());
            definition.setDefaultLedgerType(seed.ledgerType());
            definition.setDisplayOrder(seed.displayOrder());
            definition.setActive(true);
            definition.setSystemDefined(true);
            definitionRepository.save(definition);
        }
    }

    private void migrateLegacyGroups(
            CompanyEntity company,
            Map<String, AccountTypeDefinitionEntity> definitions
    ) {
        for (AccountGroupEntity group : accountGroupRepository.findAllByCompany_Id(company.getId())) {
            if (group.getAccountTypeDefinition() != null) {
                continue;
            }
            String definitionCode = LEGACY_GROUP_MAPPING.getOrDefault(
                    group.getCode(),
                    fallbackDefinition(group.getAccountType().getCode())
            );
            AccountTypeDefinitionEntity definition = definitions.get(definitionCode);
            group.setAccountTypeDefinition(definition);
            accountGroupRepository.save(group);
        }
    }

    private AccountGroupEntity ensureCompanyGroup(
            CompanyEntity company,
            AccountTypeDefinitionEntity definition
    ) {
        return accountGroupRepository
                .findFirstByCompany_IdAndAccountTypeDefinition_IdOrderByIdAsc(
                        company.getId(),
                        definition.getId()
                )
                .orElseGet(() -> {
                    AccountGroupEntity group = accountGroupRepository
                            .findByCompany_IdAndCodeIgnoreCase(company.getId(), definition.getCode())
                            .orElseGet(AccountGroupEntity::new);
                    group.setCompany(company);
                    group.setAccountType(definition.getAccountGroup());
                    group.setAccountTypeDefinition(definition);
                    group.setParentGroup(null);
                    group.setCode(definition.getCode());
                    group.setName(definition.getName());
                    group.setDisplayOrder(definition.getDisplayOrder());
                    group.setActive(true);
                    group.setSystemDefined(true);
                    return accountGroupRepository.save(group);
                });
    }

    private void ensureLedger(
            CompanyEntity company,
            AccountTypeDefinitionEntity definition,
            String code,
            String name,
            LocalDate openingDate
    ) {
        AccountGroupEntity companyGroup = ensureCompanyGroup(company, definition);
        LedgerEntity ledger = ledgerRepository.findByCompany_IdAndCodeIgnoreCase(company.getId(), code)
                .orElseGet(LedgerEntity::new);
        ledger.setCompany(company);
        ledger.setAccountGroup(companyGroup);
        ledger.setCode(code);
        ledger.setName(name);
        ledger.setLedgerType(definition.getDefaultLedgerType());
        ledger.setNormalBalance(definition.getAccountGroup().getNormalBalance());
        ledger.setPostingAllowed(true);
        ledger.setManualPostingAllowed(true);
        ledger.setControlAccount(false);
        ledger.setActive(true);
        ledger.setSystemDefined(true);
        if (ledger.getOpeningBalance() == null) {
            ledger.setOpeningBalance(BigDecimal.ZERO);
        }
        if (ledger.getOpeningBalanceAsOfDate() == null) {
            ledger.setOpeningBalanceAsOfDate(openingDate);
        }
        ledgerRepository.save(ledger);
    }

    private String fallbackDefinition(String accountGroupCode) {
        return switch (accountGroupCode) {
            case "ASSET" -> "OTHER_ASSETS";
            case "LIABILITY" -> "OTHER_CURRENT_LIABILITIES";
            case "EQUITY" -> "EQUITY";
            case "INCOME" -> "INCOME";
            case "EXPENSE" -> "EXPENSES";
            default -> throw new IllegalStateException("Unsupported account group: " + accountGroupCode);
        };
    }

    private LocalDate resolveOpeningDate(CompanyEntity company) {
        try {
            return company.getFinancialYearStart() == null
                    ? LocalDate.now()
                    : LocalDate.parse(company.getFinancialYearStart());
        } catch (DateTimeParseException ignored) {
            return LocalDate.now();
        }
    }

    private AccountTypeDefinitionSeed seed(
            String group,
            String code,
            String name,
            LedgerType ledgerType,
            Integer displayOrder
    ) {
        return new AccountTypeDefinitionSeed(group, code, name, ledgerType, displayOrder);
    }

    private record AccountGroupSeed(
            String code,
            String name,
            NormalBalance normalBalance,
            StatementType statementType,
            Integer displayOrder
    ) {
    }

    private record AccountTypeDefinitionSeed(
            String accountGroupCode,
            String code,
            String name,
            LedgerType ledgerType,
            Integer displayOrder
    ) {
    }
}
