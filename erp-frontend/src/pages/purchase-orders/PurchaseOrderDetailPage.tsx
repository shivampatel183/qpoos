import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { ApiError } from "@/lib/apiClient";
import {
  purchaseOrderService,
  type PurchaseOrder,
} from "@/services/purchaseOrderService";
import "./PurchaseOrderPage.css";

export function PurchaseOrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const popup = useMessagePopup();
  const [order, setOrder] = useState<PurchaseOrder | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    purchaseOrderService
      .get(Number(orderId), controller.signal)
      .then(setOrder)
      .catch((error) => {
        if (!controller.signal.aborted)
          popup.error(
            error instanceof ApiError
              ? error.message
              : "Failed to load purchase order.",
          );
      });
    return () => controller.abort();
  }, [orderId, popup]);
  async function action(name: "submit" | "approve" | "cancel") {
    if (!order) return;
    try {
      const updated = await purchaseOrderService[name](order.id);
      setOrder(updated);
      popup.success(`Purchase order ${name}ed.`);
    } catch (error) {
      popup.error(error instanceof ApiError ? error.message : "Action failed.");
    }
  }
  if (!order)
    return (
      <div className="purchase-order-page__loading">
        Loading purchase order...
      </div>
    );
  return (
    <div className="purchase-order-page">
      <button
        className="purchase-order-page__back"
        onClick={() => navigate("/purchase-orders")}
      >
        <ArrowLeft size={16} /> Purchase Orders
      </button>
      <section className="purchase-order-form">
        <header>
          <div>
            <h1>{order.orderNumber}</h1>
            <p>
              {order.vendorName} · {order.status} · {order.orderDate}
            </p>
          </div>
          <div className="purchase-order-form__actions">
            {order.status === "DRAFT" && (
              <>
                <button
                  onClick={() => navigate(`/purchase-orders/${order.id}/edit`)}
                >
                  Edit
                </button>
                <button onClick={() => action("submit")}>Submit</button>
              </>
            )}
            {order.status === "SENT" && (
              <button onClick={() => action("approve")}>Approve</button>
            )}
            {["DRAFT", "SENT", "APPROVED"].includes(order.status) && (
              <button onClick={() => action("cancel")}>Cancel</button>
            )}
          </div>
        </header>
        <section>
          <h2>Items</h2>
          {order.lines.map((line) => (
            <div className="purchase-order-form__line" key={line.id}>
              <strong>{line.lineNo}</strong>
              <span>{line.productName}</span>
              <span>
                {line.quantity} {line.unit ?? ""}
              </span>
              <span>{line.rate}</span>
              <span>{line.taxRate}%</span>
              <strong>{line.amount}</strong>
            </div>
          ))}
          <p>
            Total:{" "}
            {new Intl.NumberFormat("en-IN", {
              style: "currency",
              currency: "INR",
            }).format(order.totalAmount)}
          </p>
        </section>
      </section>
    </div>
  );
}
