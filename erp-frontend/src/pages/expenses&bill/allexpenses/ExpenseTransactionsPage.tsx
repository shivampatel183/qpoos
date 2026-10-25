import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Edit2, Plus, Trash2 } from "lucide-react";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn } from "@/components/ui/DataTable";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { useCompany } from "@/context/CompanyContext";
import { ApiError } from "@/lib/apiClient";
import {
  transactionService,
  type TransactionListItem,
} from "@/services/transactionService";
import "./ExpenseTransactionsPage.css";

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value ?? 0));
}

function formatDate(value: string | null): string {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

export function ExpenseTransactionsPage() {
  const navigate = useNavigate();
  const { currentCompany } = useCompany();
  const messagePopup = useMessagePopup();
  const [transactions, setTransactions] = useState<TransactionListItem[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setTransactions(await transactionService.list(signal));
      } catch (error) {
        if (!signal?.aborted) {
          messagePopup.error(
            getErrorMessage(error, "Failed to load expenses."),
          );
        }
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [messagePopup],
  );

  useEffect(() => {
    const controller = new AbortController();
    void loadData(controller.signal);
    return () => controller.abort();
  }, [currentCompany?.id, loadData]);

  function openCreatePage() {
    navigate("/expenses/new");
  }

  function openEditPage(transaction: TransactionListItem) {
    navigate(`/expenses/${transaction.id}/edit`);
  }

  async function handleDelete(transaction: TransactionListItem) {
    if (
      !window.confirm(
        `Delete this expense dated ${formatDate(transaction.transactionDate)}?`,
      )
    )
      return;
    try {
      await transactionService.delete(transaction.id);
      setTransactions((current) =>
        current.filter((item) => item.id !== transaction.id),
      );
      messagePopup.success("Expense deleted successfully.");
    } catch (error) {
      messagePopup.error(
        getErrorMessage(error, "Failed to delete expense."),
      );
    }
  }

  const columns = useMemo<DataTableColumn<TransactionListItem>[]>(
    () => [
      {
        key: "transactionDate",
        header: "Date",
        render: (transaction) => formatDate(transaction.transactionDate),
      },
      {
        key: "vendorName",
        header: "Vendor / Payee",
        render: (transaction) => transaction.vendorName || "-",
      },
      {
        key: "paymentDate",
        header: "Payment Date",
        hideOnMobile: true,
        render: (transaction) => formatDate(transaction.paymentDate),
      },
      {
        key: "paymentLedgerName",
        header: "Paid From",
        hideOnMobile: true,
        render: (transaction) => transaction.paymentLedgerName || "Not recorded",
      },
      {
        key: "journalEntryId",
        header: "Accounting",
        render: (transaction) =>
          transaction.journalEntryId === null ? "Not posted" : "Posted",
      },
      {
        key: "totalAmount",
        header: "Amount",
        width: "150px",
        render: (transaction) => (
          <span className="expense-transactions__amount">
            {formatMoney(transaction.totalAmount)}
          </span>
        ),
      },
    ],
    [],
  );

  return (
    <div className="expense-transactions">
      <DataTable
        data={transactions}
        columns={columns}
        rowKey="id"
        title="Expenses"
        loading={loading}
        loadingText="Loading expenses..."
        onRetry={() => loadData()}
        searchable
        searchPlaceholder="Search expenses..."
        defaultSort={{ key: "transactionDate", direction: "desc" }}
        pageSize={25}
        density="compact"
        primaryAction={{
          label: "New Expense",
          icon: Plus,
          onClick: openCreatePage,
        }}
        actions={[]}
        moreActions={[
          {
            label: "Edit expense",
            icon: Edit2,
            disabled: (transaction) => transaction.journalEntryId !== null,
            onClick: openEditPage,
          },
          {
            label: "Delete expense",
            icon: Trash2,
            danger: true,
            disabled: (transaction) => transaction.journalEntryId !== null,
            onClick: handleDelete,
          },
        ]}
        emptyTitle="No expenses found"
        emptyMessage="Create your first expense for this company."
        emptyAction={{
          label: "New Expense",
          icon: Plus,
          onClick: openCreatePage,
        }}
      />
    </div>
  );
}
