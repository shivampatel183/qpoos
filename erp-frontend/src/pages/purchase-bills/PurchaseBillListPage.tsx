import { useCallback, useEffect, useMemo, useState } from "react";
import { Edit2, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn } from "@/components/ui/DataTable";
import { useCompany } from "@/context/CompanyContext";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { ApiError } from "@/lib/apiClient";
import {
  purchaseBillService,
  type PurchaseBillListItem,
  type PurchaseBillStatus,
} from "@/services/purchaseBillService";
import "../purchase-orders/PurchaseOrderPage.css";

const STATUS_LABELS: Record<PurchaseBillStatus, string> = {
  DRAFT: "Draft",
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(value);
}

function errorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : "Failed to load purchase bills.";
}

export function PurchaseBillListPage() {
  const navigate = useNavigate();
  const { currentCompany } = useCompany();
  const popup = useMessagePopup();
  const [bills, setBills] = useState<PurchaseBillListItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setBills(await purchaseBillService.list(signal));
      } catch (error) {
        if (!signal?.aborted) popup.error(errorMessage(error));
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [popup],
  );

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [currentCompany?.id, load]);

  const columns = useMemo<DataTableColumn<PurchaseBillListItem>[]>(
    () => [
      {
        key: "billNumber",
        header: "Bill",
        render: (bill) => (
          <div>
            <strong>{bill.billNumber}</strong>
            <br />
            <small>{bill.billDate}</small>
          </div>
        ),
      },
      {
        key: "vendorName",
        header: "Vendor",
        render: (bill) => bill.vendorName,
      },
      {
        key: "status",
        header: "Status",
        render: (bill) => STATUS_LABELS[bill.status],
      },
      {
        key: "dueDate",
        header: "Due Date",
        render: (bill) => bill.dueDate ?? "-",
      },
      {
        key: "totalAmount",
        header: "Total",
        align: "right",
        render: (bill) => money(bill.totalAmount),
      },
    ],
    [],
  );

  return (
    <div className="purchase-order-page">
      <DataTable
        data={bills}
        columns={columns}
        rowKey="id"
        title="Purchase Bills"
        loading={loading}
        onRetry={() => load()}
        searchable
        searchPlaceholder="Search purchase bills..."
        defaultSort={{ key: "billDate", direction: "desc" }}
        pageSize={25}
        density="compact"
        primaryAction={{
          label: "New Purchase Bill",
          icon: Plus,
          onClick: () => navigate("/purchase-bills/new"),
        }}
        moreActions={[
          {
            label: "Open purchase bill",
            icon: Edit2,
            onClick: (bill) =>
              navigate(
                bill.status === "DRAFT"
                  ? `/purchase-bills/${bill.id}/edit`
                  : `/purchase-bills/${bill.id}`,
              ),
          },
        ]}
      />
    </div>
  );
}
