package com.qpoos.erp.accounting.accountgroup.dto;

import java.util.UUID;

public record AccountGroupResponse(
        Long id,
        UUID companyId,
        Long accountTypeId,
        String accountTypeCode,
        Long parentGroupId,
        String code,
        String name,
        Integer displayOrder,
        Boolean active,
        Boolean systemDefined
) {
}
