import { useCallback, useEffect, useMemo, useState } from "react";
import { useCompany } from "@/context/CompanyContext";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn } from "@/components/ui/DataTable";
import { ApiError } from "@/lib/apiClient";
import {
  accountingService,
  type JournalEntry,
} from "@/services/accountingService";
import "./JournalEntriesPage.css";

function errorMessage(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : "Failed to load journal entries.";
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function JournalEntriesPage() {
  const { currentCompany } = useCompany();
  const messagePopup = useMessagePopup();
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const loadEntries = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setEntries(await accountingService.listJournalEntries(signal));
      } catch (error) {
        if (!signal?.aborted) messagePopup.error(errorMessage(error));
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [messagePopup],
  );

  useEffect(() => {
    const controller = new AbortController();
    void loadEntries(controller.signal);
    return () => controller.abort();
  }, [currentCompany?.id, loadEntries]);

  const columns = useMemo<DataTableColumn<JournalEntry>[]>(
    () => [
      {
        key: "journalNumber",
        header: "Journal",
        width: "150px",
        render: (entry) => (
          <div className="journal-entry__identity">
            <strong>{entry.journalNumber}</strong>
            <span>{entry.sourceType.replaceAll("_", " ")}</span>
          </div>
        ),
      },
      {
        key: "accountingDate",
        header: "Date",
      },
      {
        key: "sourceDocumentId",
        header: "Source",
        render: (entry) => `#${entry.sourceDocumentId}`,
      },
      {
        key: "narration",
        header: "Description",
      },
      {
        key: "status",
        header: "Status",
        render: (entry) => (
          <span className="journal-entry__status">{entry.status.toLowerCase()}</span>
        ),
      },
      {
        key: "total",
        header: "Total",
        align: "right",
        render: (entry) =>
          formatMoney(
            entry.lines.reduce((total, line) => total + line.debitAmount, 0),
          ),
      },
    ],
    [],
  );

  return (
    <DataTable
      data={entries}
      columns={columns}
      rowKey="id"
      title="Journal Entries"
      subtitle="Posted accounting entries generated from company transactions."
      loading={loading}
      loadingText="Loading journal entries..."
      onRetry={() => loadEntries()}
      searchable
      searchPlaceholder="Search journal entries..."
      defaultSort={{ key: "accountingDate", direction: "desc" }}
      pageSize={25}
      density="compact"
      expandableRow={{
        label: (entry, expanded) => `${expanded ? "Collapse" : "Show"} ${entry.lines.length} journal lines`,
        render: (entry) => (
          <div className="journal-lines">
            <div className="journal-lines__heading">
              <strong>Journal lines</strong>
              <span>{entry.journalNumber}</span>
            </div>
            <div className="journal-lines__grid">
              <div className="journal-lines__grid-header">
                <span>Account</span>
                <span>Debit</span>
                <span>Credit</span>
              </div>
              {entry.lines.map((line) => (
                <div className="journal-lines__grid-row" key={line.id}>
                  <span className="journal-lines__account">
                    <strong>{line.ledgerName}</strong>
                    <small>{line.description ? ` - ${line.description}` : ""}</small>
                  </span>
                  <span className="journal-lines__amount">
                    {line.debitAmount > 0 ? formatMoney(line.debitAmount) : "-"}
                  </span>
                  <span className="journal-lines__amount">
                    {line.creditAmount > 0 ? formatMoney(line.creditAmount) : "-"}
                  </span>
                </div>
              ))}
              <div className="journal-lines__grid-total">
                <strong>Total</strong>
                <strong>{formatMoney(entry.lines.reduce((total, line) => total + line.debitAmount, 0))}</strong>
                <strong>{formatMoney(entry.lines.reduce((total, line) => total + line.creditAmount, 0))}</strong>
              </div>
            </div>
          </div>
        ),
      }}
      emptyTitle="No journal entries yet"
      emptyMessage="Post a purchase bill or expense to create its accounting journal."
    />
  );
}
