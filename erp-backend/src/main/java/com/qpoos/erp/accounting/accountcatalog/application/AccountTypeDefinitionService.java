package com.qpoos.erp.accounting.accountcatalog.application;

import com.qpoos.erp.accounting.accountcatalog.domain.AccountTypeDefinitionEntity;
import com.qpoos.erp.accounting.accountcatalog.infrastructure.AccountTypeDefinitionRepository;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import com.qpoos.erp.accounting.accountcatalog.dto.AccountTypeDefinitionRequest;
import com.qpoos.erp.accounting.accountcatalog.dto.AccountTypeDefinitionResponse;
import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import com.qpoos.erp.accounting.accounttype.infrastructure.AccountTypeRepository;
import com.qpoos.erp.accounting.ledger.infrastructure.LedgerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountTypeDefinitionService {

    private final AccountTypeDefinitionRepository definitionRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional
    public List<AccountTypeDefinitionResponse> list() {
        return definitionRepository
                .findAllByActiveTrueOrderByAccountGroup_DisplayOrderAscDisplayOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountTypeDefinitionResponse create(AccountTypeDefinitionRequest request) {
        AccountTypeEntity accountGroup = getAccountGroup(request.accountGroupCode());
        String code = generateCode(request.name());
        AccountTypeDefinitionEntity definition = AccountTypeDefinitionEntity.builder()
                .accountGroup(accountGroup)
                .code(code)
                .name(normalizeName(request.name()))
                .defaultLedgerType(request.defaultLedgerType())
                .displayOrder(request.displayOrder())
                .active(true)
                .systemDefined(false)
                .build();
        return toResponse(definitionRepository.save(definition));
    }

    @Transactional
    public AccountTypeDefinitionResponse update(Long definitionId, AccountTypeDefinitionRequest request) {
        AccountTypeDefinitionEntity definition = getDefinition(definitionId);
        AccountTypeEntity requestedGroup = getAccountGroup(request.accountGroupCode());
        boolean groupChanged = !definition.getAccountGroup().getId().equals(requestedGroup.getId());
        if (groupChanged && ledgerRepository.existsByAccountGroup_AccountTypeDefinition_IdAndActiveTrue(definitionId)) {
            throw new IllegalArgumentException("Account type group cannot change while active accounts use it");
        }

        definition.setAccountGroup(requestedGroup);
        definition.setName(normalizeName(request.name()));
        definition.setDefaultLedgerType(request.defaultLedgerType());
        definition.setDisplayOrder(request.displayOrder());
        return toResponse(definitionRepository.save(definition));
    }

    @Transactional
    public void deactivate(Long definitionId) {
        AccountTypeDefinitionEntity definition = getDefinition(definitionId);
        if (ledgerRepository.existsByAccountGroup_AccountTypeDefinition_IdAndActiveTrue(definitionId)) {
            throw new IllegalArgumentException("Account type cannot be deactivated while active accounts use it");
        }
        definition.setActive(false);
        definitionRepository.save(definition);
    }

    private AccountTypeEntity getAccountGroup(String code) {
        return accountTypeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account group not found"));
    }

    private AccountTypeDefinitionEntity getDefinition(Long definitionId) {
        return definitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
    }

    private String generateCode(String name) {
        String base = normalizeName(name)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isBlank()) {
            base = "ACCOUNT_TYPE";
        }
        base = base.substring(0, Math.min(base.length(), 42));
        String candidate = base;
        int suffix = 2;
        while (definitionRepository.existsByCodeIgnoreCase(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private AccountTypeDefinitionResponse toResponse(AccountTypeDefinitionEntity definition) {
        AccountTypeEntity accountGroup = definition.getAccountGroup();
        return new AccountTypeDefinitionResponse(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                accountGroup.getId(),
                accountGroup.getCode(),
                accountGroup.getName(),
                accountGroup.getNormalBalance(),
                definition.getDefaultLedgerType(),
                definition.getDisplayOrder(),
                definition.getActive(),
                definition.getSystemDefined()
        );
    }
}
