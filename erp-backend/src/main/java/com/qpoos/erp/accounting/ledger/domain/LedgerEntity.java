package com.qpoos.erp.accounting.ledger.domain;

import com.qpoos.erp.accounting.domain.LedgerType;
import com.qpoos.erp.accounting.domain.NormalBalance;
import com.qpoos.erp.accounting.accountgroup.domain.AccountGroupEntity;
import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "ledgers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ledger_company_code", columnNames = {"company_id", "code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_group_id", nullable = false)
    private AccountGroupEntity accountGroup;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column
    private LocalDate openingBalanceAsOfDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LedgerType ledgerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NormalBalance normalBalance;

    @Builder.Default
    @Column(nullable = false)
    private Boolean postingAllowed = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean manualPostingAllowed = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean controlAccount = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean systemDefined = false;
}
