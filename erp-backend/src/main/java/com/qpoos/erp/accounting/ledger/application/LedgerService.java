package com.qpoos.erp.accounting.ledger.application;

import com.qpoos.erp.accounting.domain.LedgerType;
import com.qpoos.erp.accounting.domain.NormalBalance;
import com.qpoos.erp.accounting.ledger.domain.LedgerEntity;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.accountcatalog.domain.AccountTypeDefinitionEntity;
import com.qpoos.erp.accounting.accountcatalog.infrastructure.AccountTypeDefinitionRepository;
import com.qpoos.erp.accounting.accountgroup.domain.AccountGroupEntity;
import com.qpoos.erp.accounting.accountgroup.infrastructure.AccountGroupRepository;
import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import com.qpoos.erp.accounting.ledger.dto.LedgerRequest;
import com.qpoos.erp.accounting.ledger.dto.LedgerResponse;
import com.qpoos.erp.common.security.SecurityUtils;
import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final AccountGroupRepository accountGroupRepository;
    private final AccountTypeDefinitionRepository definitionRepository;
    private final EntityManager entityManager;

    @Transactional
    public LedgerResponse create(LedgerRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        CompanyEntity company = entityManager.getReference(CompanyEntity.class, companyId);
        AccountTypeDefinitionEntity definition = getActiveDefinition(request.accountTypeId());
        AccountGroupEntity accountGroup = getOrCreateCompanyGroup(company, definition);
        String name = normalizeName(request.name());

        LedgerEntity ledger = LedgerEntity.builder()
                .company(company)
                .accountGroup(accountGroup)
                .code(generateCode(companyId, name))
                .name(name)
                .description(blankToNull(request.description()))
                .openingBalance(request.openingBalance())
                .openingBalanceAsOfDate(request.openingBalanceAsOfDate())
                .ledgerType(definition.getDefaultLedgerType())
                .normalBalance(definition.getAccountGroup().getNormalBalance())
                .postingAllowed(true)
                .manualPostingAllowed(true)
                .controlAccount(false)
                .active(true)
                .systemDefined(false)
                .build();

        return toResponse(ledgerRepository.save(ledger));
    }

    @Transactional
    public List<LedgerResponse> list() {
        UUID companyId = SecurityUtils.getCompanyId();
        return ledgerRepository.findAllByCompany_IdAndActiveTrueOrderByNameAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LedgerResponse get(Long ledgerId) {
        return toResponse(getLedger(SecurityUtils.getCompanyId(), ledgerId));
    }

    @Transactional
    public LedgerResponse update(Long ledgerId, LedgerRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        LedgerEntity ledger = getLedger(companyId, ledgerId);
        validateUserManaged(ledger);
        AccountTypeDefinitionEntity definition = getActiveDefinition(request.accountTypeId());
        CompanyEntity company = entityManager.getReference(CompanyEntity.class, companyId);

        ledger.setAccountGroup(getOrCreateCompanyGroup(company, definition));
        ledger.setName(normalizeName(request.name()));
        ledger.setDescription(blankToNull(request.description()));
        ledger.setOpeningBalance(request.openingBalance());
        ledger.setOpeningBalanceAsOfDate(request.openingBalanceAsOfDate());
        ledger.setLedgerType(definition.getDefaultLedgerType());
        ledger.setNormalBalance(definition.getAccountGroup().getNormalBalance());

        return toResponse(ledgerRepository.save(ledger));
    }

    @Transactional
    public void deactivate(Long ledgerId) {
        LedgerEntity ledger = getLedger(SecurityUtils.getCompanyId(), ledgerId);
        validateUserManaged(ledger);
        ledger.setActive(false);
        ledgerRepository.save(ledger);
    }

    private AccountTypeDefinitionEntity getActiveDefinition(Long definitionId) {
        AccountTypeDefinitionEntity definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
        if (!Boolean.TRUE.equals(definition.getActive())) {
            throw new IllegalArgumentException("Account type is inactive");
        }
        return definition;
    }

    private AccountGroupEntity getOrCreateCompanyGroup(
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

    private LedgerEntity getLedger(UUID companyId, Long ledgerId) {
        return ledgerRepository.findByIdAndCompany_Id(ledgerId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found"));
    }

    private void validateUserManaged(LedgerEntity ledger) {
        if (Boolean.TRUE.equals(ledger.getSystemDefined())) {
            throw new IllegalArgumentException("System-defined accounts cannot be changed");
        }
    }

    private String generateCode(UUID companyId, String name) {
        String base = name.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isBlank()) {
            base = "ACCOUNT";
        }
        base = base.substring(0, Math.min(base.length(), 42));
        String candidate = base;
        int suffix = 2;
        while (ledgerRepository.existsByCompany_IdAndCodeIgnoreCase(companyId, candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LedgerResponse toResponse(LedgerEntity ledger) {
        AccountGroupEntity companyGroup = ledger.getAccountGroup();
        AccountTypeDefinitionEntity definition = companyGroup.getAccountTypeDefinition();
        AccountTypeEntity accountGroup = definition == null
                ? companyGroup.getAccountType()
                : definition.getAccountGroup();
        BigDecimal openingBalance = ledger.getOpeningBalance() == null
                ? BigDecimal.ZERO
                : ledger.getOpeningBalance();

        return new LedgerResponse(
                ledger.getId(),
                ledger.getCompany().getId(),
                accountGroup.getId(),
                accountGroup.getCode(),
                accountGroup.getName(),
                definition == null ? companyGroup.getId() : definition.getId(),
                definition == null ? companyGroup.getCode() : definition.getCode(),
                definition == null ? companyGroup.getName() : definition.getName(),
                ledger.getCode(),
                ledger.getName(),
                ledger.getDescription(),
                openingBalance,
                ledger.getOpeningBalanceAsOfDate(),
                openingBalance,
                ledger.getLedgerType(),
                ledger.getNormalBalance(),
                ledger.getActive(),
                ledger.getSystemDefined()
        );
    }
}
