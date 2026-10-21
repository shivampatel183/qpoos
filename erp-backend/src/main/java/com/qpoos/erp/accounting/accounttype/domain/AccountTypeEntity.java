package com.qpoos.erp.accounting.accounttype.domain;

import com.qpoos.erp.accounting.domain.NormalBalance;
import com.qpoos.erp.accounting.domain.StatementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NormalBalance normalBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatementType statementType;

    private Integer displayOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean systemDefined = true;
}
