import { useCallback, useEffect, useMemo, useState } from "react";
import { Edit2, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn } from "@/components/ui/DataTable";
import { useCompany } from "@/context/CompanyContext";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { ApiError } from "@/lib/apiClient";
import {
  purchaseOrderService,
  type PurchaseOrderListItem,
  type PurchaseOrderStatus,
} from "@/services/purchaseOrderService";
import "./PurchaseOrderPage.css";

const STATUS_LABELS: Record<PurchaseOrderStatus, string> = {
  DRAFT: "Draft",
  SENT: "Sent",
  APPROVED: "Approved",
  PARTIALLY_RECEIVED: "Partially Received",
  RECEIVED: "Received",
  CANCELLED: "Cancelled",
  CLOSED: "Closed",
};

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(value);
}
function errorMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "Failed to load purchase orders.";
}

export function PurchaseOrderListPage() {
  const navigate = useNavigate();
  const { currentCompany } = useCompany();
  const popup = useMessagePopup();
  const [orders, setOrders] = useState<PurchaseOrderListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setOrders(await purchaseOrderService.list(signal));
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
  const columns = useMemo<DataTableColumn<PurchaseOrderListItem>[]>(
    () => [
      {
        key: "orderNumber",
        header: "Order",
        render: (order) => (
          <div>
            <strong>{order.orderNumber}</strong>
            <br />
            <small>{order.orderDate}</small>
          </div>
        ),
      },
      {
        key: "vendorName",
        header: "Vendor",
        render: (order) => order.vendorName,
      },
      {
        key: "status",
        header: "Status",
        render: (order) => STATUS_LABELS[order.status],
      },
      {
        key: "expectedDeliveryDate",
        header: "Expected Delivery",
        render: (order) => order.expectedDeliveryDate ?? "-",
      },
      {
        key: "totalAmount",
        header: "Total",
        align: "left",
        render: (order) => money(order.totalAmount),
      },
    ],
    [],
  );
  return (
    <div className="purchase-order-page">
      <DataTable
        data={orders}
        columns={columns}
        rowKey="id"
        title="Purchase Orders"
        loading={loading}
        onRetry={() => load()}
        searchable
        searchPlaceholder="Search purchase orders..."
        defaultSort={{ key: "orderDate", direction: "desc" }}
        pageSize={25}
        density="compact"
        primaryAction={{
          label: "New Purchase Order",
          icon: Plus,
          onClick: () => navigate("/purchase-orders/new"),
        }}
        moreActions={[
          {
            label: "Open purchase order",
            icon: Edit2,
            onClick: (order) =>
              navigate(
                order.status === "DRAFT"
                  ? `/purchase-orders/${order.id}/edit`
                  : `/purchase-orders/${order.id}`,
              ),
          },
        ]}
      />
    </div>
  );
}
