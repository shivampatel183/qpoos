import { useCallback, useEffect, useMemo, useState } from "react";
import { Edit2, Plus, Trash2 } from "lucide-react";
import { AccountFormModal } from "@/components/accounting";
import { DataTable } from "@/components/ui/DataTable";
import type {
  DataTableColumn,
  DataTableFilter,
} from "@/components/ui/DataTable";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { useCompany } from "@/context/CompanyContext";
import { ApiError } from "@/lib/apiClient";
import { accountingService, type Ledger } from "@/services/accountingService";
import "./ChartOfAccountsPage.css";

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatBalance(ledger: Ledger): string {
  const amount = new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(ledger.balance);
  return `${amount} ${"₹"}`;
}

export function ChartOfAccountsPage() {
  const { currentCompany } = useCompany();
  const messagePopup = useMessagePopup();
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editingLedger, setEditingLedger] = useState<Ledger | null>(null);

  const loadData = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setLedgers(await accountingService.listLedgers(signal));
      } catch (error) {
        if (!signal?.aborted) {
          messagePopup.error(
            getErrorMessage(error, "Failed to load chart of accounts."),
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

  function openCreateModal() {
    setEditingLedger(null);
    setModalMode("create");
  }

  function openEditModal(ledger: Ledger) {
    setEditingLedger(ledger);
    setModalMode("edit");
  }

  function closeModal() {
    setModalMode(null);
    setEditingLedger(null);
  }

  function handleAccountSaved(savedAccount: Ledger) {
    setLedgers((current) => {
      const existingIndex = current.findIndex(
        (account) => account.id === savedAccount.id,
      );
      if (existingIndex < 0) return [...current, savedAccount];
      return current.map((account) =>
        account.id === savedAccount.id ? savedAccount : account,
      );
    });
  }

  async function handleDelete(ledger: Ledger) {
    if (!window.confirm(`Deactivate ${ledger.name}?`)) return;
    try {
      await accountingService.deleteLedger(ledger.id);
      messagePopup.success("Account deactivated successfully.");
      await loadData();
    } catch (error) {
      messagePopup.error(
        getErrorMessage(error, "Failed to deactivate account."),
      );
    }
  }

  const columns = useMemo<DataTableColumn<Ledger>[]>(
    () => [
      {
        key: "name",
        header: "Name",
        render: (ledger) => (
          <div className="coa-account-cell">
            <span>{ledger.name}</span>
          </div>
        ),
      },
      {
        key: "accountGroupName",
        header: "Account Group",
        render: (ledger) => ledger.accountGroupName,
      },
      {
        key: "accountTypeName",
        header: "Account Type",
        render: (ledger) => ledger.accountTypeName,
      },
      {
        key: "description",
        header: "Description",
        hideOnMobile: true,
        render: (ledger) => (
          <span className="coa-description">{ledger.description || "-"}</span>
        ),
      },
      {
        key: "balance",
        header: "Balance",
        width: "150px",
        render: (ledger) => (
          <span className="coa-balance">{formatBalance(ledger)}</span>
        ),
      },
    ],
    [],
  );

  const filters = useMemo<DataTableFilter<Ledger>[]>(
    () => [
      {
        key: "accountGroupName",
        label: "Account Group",
        allLabel: "All Account Groups",
      },
      {
        key: "accountTypeName",
        label: "Account Type",
        allLabel: "All Account Types",
      },
    ],
    [],
  );

  return (
    <div className="coa-page">
      {/* <div className="coa-header">
        <div>
          <h1 className="coa-title">Chart of Accounts</h1>
        </div>
      </div> */}

      <DataTable
        data={ledgers}
        columns={columns}
        rowKey="id"
        title="Chart of Accounts"
        loading={loading}
        loadingText="Loading accounts..."
        onRetry={() => loadData()}
        searchable
        searchPlaceholder="Search accounts..."
        filters={filters}
        defaultSort={{ key: "name", direction: "asc" }}
        pageSize={25}
        density="compact"
        primaryAction={{
          label: "New Account",
          icon: Plus,
          onClick: openCreateModal,
        }}
        actions={[]}
        moreActions={[
          {
            label: "Edit account",
            icon: Edit2,
            onClick: openEditModal,
          },
          {
            label: "Deactivate account",
            icon: Trash2,
            danger: true,
            onClick: handleDelete,
          },
        ]}
        emptyTitle="No accounts found"
        emptyMessage="Create your first account for this company."
        emptyAction={{
          label: "New Account",
          icon: Plus,
          onClick: openCreateModal,
        }}
      />

      {modalMode && (
        <AccountFormModal
          account={
            modalMode === "edit" ? (editingLedger ?? undefined) : undefined
          }
          onClose={closeModal}
          onSaved={handleAccountSaved}
        />
      )}
    </div>
  );
}
