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
        render: (entry) => (
          <div>
            <strong>{entry.journalNumber}</strong>
            <br />
            <small>{entry.sourceType.replaceAll("_", " ")}</small>
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
        render: (entry) => `${entry.sourceType} #${entry.sourceDocumentId}`,
      },
      {
        key: "narration",
        header: "Description",
      },
      {
        key: "lines",
        header: "Lines",
        render: (entry) => (
          <details>
            <summary>{entry.lines.length} lines</summary>
            <ul>
              {entry.lines.map((line) => (
                <li key={line.id}>
                  {line.ledgerCode} - {line.ledgerName}:{" "}
                  {formatMoney(line.debitAmount)} Dr /{" "}
                  {formatMoney(line.creditAmount)} Cr
                </li>
              ))}
            </ul>
          </details>
        ),
      },
      {
        key: "status",
        header: "Status",
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
      emptyTitle="No journal entries yet"
      emptyMessage="Post a purchase bill or expense to create its accounting journal."
    />
  );
}
