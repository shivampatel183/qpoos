import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowRight, Edit2, Plus, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableAction, DataTableColumn } from "@/components/ui/DataTable";
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
  SENT: "Sent to vendor",
  APPROVED: "Approved",
  RECEIVED: "Received",
  CANCELLED: "Cancelled",
  CLOSED: "Closed",
};

const PURCHASE_ORDER_STAGES: PurchaseOrderStatus[] = [
  "DRAFT",
  "SENT",
  "APPROVED",
  "RECEIVED",
  "CLOSED",
];

const NEXT_STAGE_ACTION: Partial<
  Record<PurchaseOrderStatus, "submit" | "approve" | "receive" | "close">
> = {
  DRAFT: "submit",
  SENT: "approve",
  APPROVED: "receive",
  RECEIVED: "close",
};

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(value);
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function PurchaseOrderStage({ status }: { status: PurchaseOrderStatus }) {
  if (status === "CANCELLED") {
    return (
      <div className="purchase-order-stage">
        <span className="purchase-order-stage__badge purchase-order-stage__badge--cancelled">
          {STATUS_LABELS[status]}
        </span>
        <small className="purchase-order-stage__detail">Order stopped</small>
      </div>
    );
  }

  const currentIndex = PURCHASE_ORDER_STAGES.indexOf(status);
  return (
    <div
      className="purchase-order-stage"
      aria-label={`Current stage: ${STATUS_LABELS[status]}, stage ${currentIndex + 1} of ${PURCHASE_ORDER_STAGES.length}`}
    >
      <span className={`purchase-order-stage__badge purchase-order-stage__badge--${status.toLowerCase().replaceAll("_", "-")}`}>
        {STATUS_LABELS[status]}
      </span>
      <div className="purchase-order-stage__track" aria-hidden="true">
        {PURCHASE_ORDER_STAGES.map((stage, index) => (
          <span
            key={stage}
            className={`purchase-order-stage__step${index <= currentIndex ? " purchase-order-stage__step--complete" : ""}${index === currentIndex ? " purchase-order-stage__step--current" : ""}`}
          />
        ))}
      </div>
      <small className="purchase-order-stage__detail">
        Stage {currentIndex + 1} of {PURCHASE_ORDER_STAGES.length}
      </small>
    </div>
  );
}

export function PurchaseOrderListPage() {
  const navigate = useNavigate();
  const { currentCompany } = useCompany();
  const popup = useMessagePopup();
  const [orders, setOrders] = useState<PurchaseOrderListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingOrderId, setUpdatingOrderId] = useState<number | null>(null);
  const load = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setOrders(await purchaseOrderService.list(signal));
      } catch (error) {
        if (!signal?.aborted) {
          popup.error(errorMessage(error, "Failed to load purchase orders."));
        }
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

  const updateStage = useCallback(
    async (
      order: PurchaseOrderListItem,
      action: "submit" | "approve" | "receive" | "close" | "cancel",
    ) => {
      setUpdatingOrderId(order.id);
      try {
        const updated = await purchaseOrderService[action](order.id);
        setOrders((current) =>
          current.map((item) =>
            item.id === order.id ? { ...item, status: updated.status } : item,
          ),
        );
        popup.success(`Purchase order moved to ${STATUS_LABELS[updated.status]}.`);
      } catch (error) {
        popup.error(errorMessage(error, "Failed to update purchase order stage."));
      } finally {
        setUpdatingOrderId(null);
      }
    },
    [popup],
  );

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
        header: "Current stage",
        render: (order) => <PurchaseOrderStage status={order.status} />,
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

  const rowActions = useMemo<DataTableAction<PurchaseOrderListItem>[]>(
    () => [
      {
        label: "Advance to next stage",
        icon: ArrowRight,
        disabled: (order) =>
          !NEXT_STAGE_ACTION[order.status] || updatingOrderId === order.id,
        onClick: (order) => {
          const action = NEXT_STAGE_ACTION[order.status];
          if (action) void updateStage(order, action);
        },
      },
    ],
    [updateStage, updatingOrderId],
  );

  const moreActions = useMemo<DataTableAction<PurchaseOrderListItem>[]>(
    () => [
      {
        label: "Open order",
        icon: Edit2,
        onClick: (order) =>
          navigate(
            order.status === "DRAFT"
              ? `/purchase-orders/${order.id}/edit`
              : `/purchase-orders/${order.id}`,
          ),
      },
      {
        label: "Cancel order",
        icon: X,
        danger: true,
        hidden: (order) => !["DRAFT", "SENT", "APPROVED"].includes(order.status),
        disabled: (order) => updatingOrderId === order.id,
        onClick: (order) => void updateStage(order, "cancel"),
      },
    ],
    [navigate, updateStage, updatingOrderId],
  );

  return (
    <div className="purchase-order-page">
      <DataTable
        data={orders}
        columns={columns}
        rowKey="id"
        title="Purchase Orders"
        subtitle="Track each order from draft through delivery."
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
        actions={rowActions}
        moreActions={moreActions}
      />
    </div>
  );
}
