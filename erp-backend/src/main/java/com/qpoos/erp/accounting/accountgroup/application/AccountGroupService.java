package com.qpoos.erp.accounting.accountgroup.application;

import com.qpoos.erp.accounting.accountgroup.domain.AccountGroupEntity;
import com.qpoos.erp.accounting.accountgroup.infrastructure.AccountGroupRepository;
import com.qpoos.erp.accounting.accountgroup.dto.AccountGroupRequest;
import com.qpoos.erp.accounting.accountgroup.dto.AccountGroupResponse;
import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import com.qpoos.erp.accounting.accounttype.infrastructure.AccountTypeRepository;
import com.qpoos.erp.company.domain.CompanyEntity;
import com.qpoos.erp.common.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountGroupService {

    private final AccountGroupRepository accountGroupRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final EntityManager entityManager;

    @Transactional
    public AccountGroupResponse create(AccountGroupRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        CompanyEntity company = entityManager.getReference(CompanyEntity.class, companyId);
        AccountTypeEntity accountType = getAccountType(request.accountTypeCode());
        AccountGroupEntity parentGroup = getParentGroup(companyId, request.parentGroupId(), accountType);
        String code = normalizeCode(request.code());

        if (accountGroupRepository.existsByCompany_IdAndCodeIgnoreCase(companyId, code)) {
            throw new IllegalArgumentException("Account group code already exists");
        }

        AccountGroupEntity group = AccountGroupEntity.builder()
                .company(company)
                .accountType(accountType)
                .parentGroup(parentGroup)
                .code(code)
                .name(normalizeName(request.name()))
                .displayOrder(request.displayOrder())
                .active(true)
                .systemDefined(false)
                .build();

        return toResponse(accountGroupRepository.save(group));
    }

    @Transactional
    public List<AccountGroupResponse> list() {
        UUID companyId = SecurityUtils.getCompanyId();
        return accountGroupRepository.findAllByCompany_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountGroupResponse get(Long groupId) {
        UUID companyId = SecurityUtils.getCompanyId();
        return toResponse(getGroup(companyId, groupId));
    }

    @Transactional
    public AccountGroupResponse update(Long groupId, AccountGroupRequest request) {
        UUID companyId = SecurityUtils.getCompanyId();
        AccountGroupEntity group = getGroup(companyId, groupId);
        AccountTypeEntity accountType = getAccountType(request.accountTypeCode());
        AccountGroupEntity parentGroup = getParentGroup(companyId, request.parentGroupId(), accountType);
        String code = normalizeCode(request.code());

        if (accountGroupRepository.existsByCompany_IdAndCodeIgnoreCaseAndIdNot(companyId, code, groupId)) {
            throw new IllegalArgumentException("Account group code already exists");
        }
        if (parentGroup != null && parentGroup.getId().equals(groupId)) {
            throw new IllegalArgumentException("Account group cannot be its own parent");
        }
        if (parentGroup != null && createsCycle(groupId, parentGroup)) {
            throw new IllegalArgumentException("Circular account group hierarchy is not allowed");
        }

        group.setAccountType(accountType);
        group.setParentGroup(parentGroup);
        group.setCode(code);
        group.setName(normalizeName(request.name()));
        group.setDisplayOrder(request.displayOrder());

        return toResponse(accountGroupRepository.save(group));
    }

    @Transactional
    public void deactivate(Long groupId) {
        UUID companyId = SecurityUtils.getCompanyId();
        AccountGroupEntity group = getGroup(companyId, groupId);
        group.setActive(false);
        accountGroupRepository.save(group);
    }

    private AccountTypeEntity getAccountType(String accountTypeCode) {
        return accountTypeRepository.findByCodeIgnoreCase(normalizeCode(accountTypeCode))
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
    }

    private AccountGroupEntity getGroup(UUID companyId, Long groupId) {
        return accountGroupRepository.findByIdAndCompany_Id(groupId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Account group not found"));
    }

    private AccountGroupEntity getParentGroup(UUID companyId, Long parentGroupId, AccountTypeEntity accountType) {
        if (parentGroupId == null) {
            return null;
        }
        AccountGroupEntity parentGroup = getGroup(companyId, parentGroupId);
        if (!parentGroup.getAccountType().getId().equals(accountType.getId())) {
            throw new IllegalArgumentException("Parent account group must have the same account type");
        }
        return parentGroup;
    }

    private boolean createsCycle(Long groupId, AccountGroupEntity parentGroup) {
        AccountGroupEntity cursor = parentGroup;
        while (cursor != null) {
            if (cursor.getId().equals(groupId)) {
                return true;
            }
            cursor = cursor.getParentGroup();
        }
        return false;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private AccountGroupResponse toResponse(AccountGroupEntity group) {
        AccountGroupEntity parentGroup = group.getParentGroup();
        return new AccountGroupResponse(
                group.getId(),
                group.getCompany().getId(),
                group.getAccountType().getId(),
                group.getAccountType().getCode(),
                parentGroup == null ? null : parentGroup.getId(),
                group.getCode(),
                group.getName(),
                group.getDisplayOrder(),
                group.getActive(),
                group.getSystemDefined()
        );
    }
}
