package com.qpoos.erp.common.number;

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
        name = "document_number_sequences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_number_sequence_scope",
                columnNames = {"company_id", "document_type", "financial_year"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentNumberSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber;
}