import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiConfig";

export type PurchaseOrderStatus =
  | "DRAFT"
  | "SENT"
  | "APPROVED"
  | "RECEIVED"
  | "CANCELLED"
  | "CLOSED";

export interface DeliveryAddress {
  line1: string | null;
  line2: string | null;
  city: string | null;
  state: string | null;
  country: string | null;
  postalCode: string | null;
}

export interface PaymentTerm {
  id: number;
  code: string;
  name: string;
  dueDays: number;
  active: boolean;
}

export interface PurchaseOrderLine {
  id?: number;
  lineNo: number;
  productId: number;
  productName?: string;
  description: string | null;
  quantity: number;
  unit: string | null;
  rate: number;
  discount: number;
  taxRate: number;
  taxAmount?: number;
  amount?: number;
}

export interface PurchaseOrderListItem {
  id: number;
  orderNumber: string;
  orderDate: string;
  vendorId: number;
  vendorName: string;
  status: PurchaseOrderStatus;
  totalAmount: number;
  expectedDeliveryDate: string | null;
}

export interface PurchaseOrder extends PurchaseOrderListItem {
  paymentTermId: number | null;
  paymentTermCode: string | null;
  deliveryTerm: string | null;
  deliveryAddress: DeliveryAddress | null;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  roundOff: number;
  notes: string | null;
  attachments: string | null;
  createdAt: string;
  updatedAt: string;
  lines: PurchaseOrderLine[];
}

export interface PurchaseOrderPayload {
  orderDate: string;
  vendorId: number;
  paymentTermId: number | null;
  deliveryTerm: string | null;
  expectedDeliveryDate: string | null;
  deliveryAddress: DeliveryAddress | null;
  roundOff: number;
  notes: string | null;
  attachments: string | null;
  lines: PurchaseOrderLine[];
}

export const purchaseOrderService = {
  list(signal?: AbortSignal): Promise<PurchaseOrderListItem[]> {
    return apiClient.get<PurchaseOrderListItem[]>(API.purchaseOrders.list, {
      signal,
    });
  },
  get(id: number, signal?: AbortSignal): Promise<PurchaseOrder> {
    return apiClient.get<PurchaseOrder>(API.purchaseOrders.detail(id), {
      signal,
    });
  },
  create(payload: PurchaseOrderPayload): Promise<PurchaseOrder> {
    return apiClient.post<PurchaseOrder>(API.purchaseOrders.create, payload);
  },
  update(id: number, payload: PurchaseOrderPayload): Promise<PurchaseOrder> {
    return apiClient.put<PurchaseOrder>(API.purchaseOrders.update(id), payload);
  },
  submit(id: number): Promise<PurchaseOrder> {
    return apiClient.post<PurchaseOrder>(API.purchaseOrders.submit(id), {});
  },
  approve(id: number): Promise<PurchaseOrder> {
    return apiClient.post<PurchaseOrder>(API.purchaseOrders.approve(id), {});
  },
  receive(id: number): Promise<PurchaseOrder> {
    return apiClient.post<PurchaseOrder>(API.purchaseOrders.receive(id), {});
  },
  close(id: number): Promise<PurchaseOrder> {
    return apiClient.post<PurchaseOrder>(API.purchaseOrders.close(id), {});
  },
  cancel(id: number): Promise<PurchaseOrder> {
    return apiClient.post<PurchaseOrder>(API.purchaseOrders.cancel(id), {});
  },
};

export const paymentTermService = {
  list(signal?: AbortSignal): Promise<PaymentTerm[]> {
    return apiClient.get<PaymentTerm[]>(API.paymentTerms.list, { signal });
  },
};
