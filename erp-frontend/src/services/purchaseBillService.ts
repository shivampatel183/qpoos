import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiConfig";

export type PurchaseBillStatus = "DRAFT" | "POSTED" | "CANCELLED";

export interface PaymentTerm {
  id: number;
  code: string;
  name: string;
  dueDays: number;
  active: boolean;
}

export interface PurchaseBillLine {
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

export interface PurchaseBillListItem {
  id: number;
  billNumber: string;
  billDate: string;
  vendorId: number;
  vendorName: string;
  status: PurchaseBillStatus;
  totalAmount: number;
  dueDate: string | null;
}

export interface PurchaseBill extends PurchaseBillListItem {
  paymentTermId: number | null;
  paymentTermCode: string | null;
  purchaseOrderId: number | null;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  roundOff: number;
  postedVoucherId: number | null;
  journalEntryId: number | null;
  notes: string | null;
  attachments: string | null;
  createdAt: string;
  updatedAt: string;
  lines: PurchaseBillLine[];
}

export interface PurchaseBillPayload {
  billDate: string;
  vendorId: number;
  paymentTermId: number | null;
  dueDate: string | null;
  purchaseOrderId: number | null;
  roundOff: number;
  notes: string | null;
  attachments: string | null;
  lines: PurchaseBillLine[];
}

export const purchaseBillService = {
  list(signal?: AbortSignal): Promise<PurchaseBillListItem[]> {
    return apiClient.get<PurchaseBillListItem[]>(API.purchaseBills.list, {
      signal,
    });
  },
  get(id: number, signal?: AbortSignal): Promise<PurchaseBill> {
    return apiClient.get<PurchaseBill>(API.purchaseBills.detail(id), {
      signal,
    });
  },
  create(payload: PurchaseBillPayload): Promise<PurchaseBill> {
    return apiClient.post<PurchaseBill>(API.purchaseBills.create, payload);
  },
  update(id: number, payload: PurchaseBillPayload): Promise<PurchaseBill> {
    return apiClient.put<PurchaseBill>(API.purchaseBills.update(id), payload);
  },
  post(id: number): Promise<PurchaseBill> {
    return apiClient.post<PurchaseBill>(API.purchaseBills.post(id), {});
  },
  cancel(id: number): Promise<PurchaseBill> {
    return apiClient.post<PurchaseBill>(API.purchaseBills.cancel(id), {});
  },
};

export const paymentTermService = {
  list(signal?: AbortSignal): Promise<{ id: number; code: string; name: string; dueDays: number; active: boolean }[]> {
    return apiClient.get<{ id: number; code: string; name: string; dueDays: number; active: boolean }[]>(API.paymentTerms.list, { signal });
  },
};
