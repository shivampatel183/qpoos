package com.qpoos.erp.accounting.accounttype.application;

import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import com.qpoos.erp.accounting.accounttype.dto.AccountTypeResponse;
import com.qpoos.erp.accounting.accounttype.infrastructure.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    public List<AccountTypeResponse> list() {
        return accountTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private AccountTypeResponse toResponse(AccountTypeEntity accountType) {
        return new AccountTypeResponse(
                accountType.getId(),
                accountType.getCode(),
                accountType.getName(),
                accountType.getNormalBalance(),
                accountType.getStatementType(),
                accountType.getDisplayOrder(),
                accountType.getSystemDefined()
        );
    }
}
