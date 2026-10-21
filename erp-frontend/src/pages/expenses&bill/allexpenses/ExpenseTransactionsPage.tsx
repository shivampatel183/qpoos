import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Edit2, Plus, Receipt, ShoppingCart, Trash2 } from "lucide-react";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn } from "@/components/ui/DataTable";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { useCompany } from "@/context/CompanyContext";
import { ApiError } from "@/lib/apiClient";
import {
  transactionService,
  type PaymentStatus,
  type TransactionListItem,
  type TransactionType,
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

const TYPE_LABELS: Record<TransactionType, string> = {
  PURCHASE_BILL: "Purchase Bill",
  PURCHASE_ORDER: "Purchase Order",
  PURCHASE_RETURN: "Purchase Return",
  EXPENSE: "Expense",
  PAYMENT: "Payment",
};

const STATUS_LABELS: Record<PaymentStatus, string> = {
  UNPAID: "Unpaid",
  PARTIALLY_PAID: "Partially Paid",
  PAID: "Paid",
  OVERDUE: "Overdue",
  CANCELLED: "Cancelled",
};

const STATUS_CLASS: Record<PaymentStatus, string> = {
  UNPAID: "expense-transactions__status expense-transactions__status--unpaid",
  PARTIALLY_PAID:
    "expense-transactions__status expense-transactions__status--partial",
  PAID: "expense-transactions__status expense-transactions__status--paid",
  OVERDUE: "expense-transactions__status expense-transactions__status--overdue",
  CANCELLED:
    "expense-transactions__status expense-transactions__status--cancelled",
};

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
            getErrorMessage(error, "Failed to load transactions."),
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

  function openPurchaseOrderPage() {
    navigate("/purchase-orders/new");
  }

  function openBillPage() {
    navigate("/purchase-bills/new");
  }

  function openEditPage(transaction: TransactionListItem) {
    navigate(`/expenses/${transaction.id}/edit`);
  }

  async function handleDelete(transaction: TransactionListItem) {
    if (
      !window.confirm(
        `Delete this transaction dated ${formatDate(transaction.transactionDate)}?`,
      )
    )
      return;
    try {
      await transactionService.delete(transaction.id);
      setTransactions((current) =>
        current.filter((item) => item.id !== transaction.id),
      );
      messagePopup.success("Transaction deleted successfully.");
    } catch (error) {
      messagePopup.error(
        getErrorMessage(error, "Failed to delete transaction."),
      );
    }
  }

  const columns = useMemo<DataTableColumn<TransactionListItem>[]>(
    () => [
      // {
      //     key: 'billNo',
      //     header: 'Bill',
      //     render: (transaction) => (
      //         <div className="expense-transactions__name">
      //             <strong>{transaction.billNo || '-'}</strong>
      //             <span>{formatDate(transaction.transactionDate)}</span>
      //         </div>
      //     ),
      // },
      {
        key: "transactionDate",
        header: "Date",
        render: (transaction) => formatDate(transaction.transactionDate),
      },
      {
        key: "transactionType",
        header: "Type",
        render: (transaction) => TYPE_LABELS[transaction.transactionType],
      },
      {
        key: "vendorName",
        header: "Vendor",
        render: (transaction) => transaction.vendorName || "-",
      },
      {
        key: "dueDate",
        header: "Due Date",
        hideOnMobile: true,
        render: (transaction) => formatDate(transaction.dueDate),
      },
      {
        key: "paymentDate",
        header: "Payment Date",
        hideOnMobile: true,
        render: (transaction) => formatDate(transaction.paymentDate),
      },
      {
        key: "paymentStatus",
        header: "Status",
        render: (transaction) => (
          <span className={STATUS_CLASS[transaction.paymentStatus]}>
            {STATUS_LABELS[transaction.paymentStatus]}
          </span>
        ),
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
        title="Expense Transactions"
        loading={loading}
        loadingText="Loading transactions..."
        onRetry={() => loadData()}
        searchable
        searchPlaceholder="Search transactions..."
        defaultSort={{ key: "transactionDate", direction: "desc" }}
        pageSize={25}
        density="compact"
        primaryAction={{
          label: "New Expense",
          icon: Plus,
          onClick: openCreatePage,
          menu: [
            {
              label: "Expense",
              icon: Plus,
              onClick: openCreatePage,
            },
            {
              label: "Purchase Order",
              icon: ShoppingCart,
              onClick: openPurchaseOrderPage,
            },
            {
              label: "Bill",
              icon: Receipt,
              onClick: openBillPage,
            },
          ],
        }}
        actions={[]}
        moreActions={[
          {
            label: "Edit transaction",
            icon: Edit2,
            onClick: openEditPage,
          },
          {
            label: "Delete transaction",
            icon: Trash2,
            danger: true,
            onClick: handleDelete,
          },
        ]}
        filters={[
          {
            key: "transactionType",
            label: "Type",
            allLabel: "All types",
            getValue: (transaction) => transaction.transactionType,
            options: Object.entries(TYPE_LABELS).map(([value, label]) => ({
              value,
              label,
            })),
          },
          {
            key: "paymentStatus",
            label: "Status",
            allLabel: "All statuses",
            getValue: (transaction) => transaction.paymentStatus,
            options: Object.entries(STATUS_LABELS).map(([value, label]) => ({
              value,
              label,
            })),
          },
        ]}
        emptyTitle="No transactions found"
        emptyMessage="Create your first expense transaction for this company."
        emptyAction={{
          label: "New Expense",
          icon: Plus,
          onClick: openCreatePage,
        }}
      />
    </div>
  );
}
