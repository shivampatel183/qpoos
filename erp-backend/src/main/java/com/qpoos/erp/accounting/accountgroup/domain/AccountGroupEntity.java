package com.qpoos.erp.accounting.accountgroup.domain;

import com.qpoos.erp.accounting.accountcatalog.domain.AccountTypeDefinitionEntity;
import com.qpoos.erp.accounting.accounttype.domain.AccountTypeEntity;
import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "account_groups",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_group_company_code", columnNames = {"company_id", "code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountTypeEntity accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_definition_id")
    private AccountTypeDefinitionEntity accountTypeDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private AccountGroupEntity parentGroup;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    private Integer displayOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean systemDefined = false;
}
