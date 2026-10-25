package com.qpoos.erp.accounting.journal.domain;

import com.qpoos.erp.company.domain.CompanyEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "journal_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_journal_company_number",
                        columnNames = {"company_id", "journal_number"}
                ),
                @UniqueConstraint(
                        name = "uk_journal_source_event",
                        columnNames = {"company_id", "source_type", "source_document_id", "entry_kind"}
                )
        },
        indexes = {
                @Index(name = "idx_journal_company_date", columnList = "company_id, accounting_date"),
                @Index(name = "idx_journal_source", columnList = "source_type, source_document_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(name = "journal_number", nullable = false, length = 80)
    private String journalNumber;

    @Column(name = "accounting_date", nullable = false)
    private LocalDate accountingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private JournalSourceType sourceType;

    @Column(name = "source_document_id", nullable = false)
    private Long sourceDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_kind", nullable = false, length = 20)
    private JournalEntryKind entryKind;

    @Column(nullable = false, length = 1000)
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "posted_by", nullable = false)
    private UUID postedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private OffsetDateTime postedAt;

    @Builder.Default
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<JournalLineEntity> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        postedAt = now;
    }

    public void addLine(JournalLineEntity line) {
        lines.add(line);
        line.setJournalEntry(this);
    }
}
