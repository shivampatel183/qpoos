import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiConfig";

export type PaymentStatus = "DRAFT" | "POSTED" | "CANCELLED" | "REVERSED";
export type PaymentMethod = "CASH" | "BANK" | "UPI" | "CHEQUE" | "CARD";

export interface PaymentAllocation {
  id: number | null;
  documentType: string;
  documentId: number;
  documentNumber: string | null;
  allocatedAmount: number;
  outstandingAmount: number | null;
}

export interface Payment {
  id: number;
  paymentNumber: string;
  paymentDate: string;
  vendorId: number;
  vendorName: string;
  paymentMethod: PaymentMethod;
  sourceLedgerId: number | null;
  sourceLedgerName: string | null;
  amount: number;
  referenceNumber: string | null;
  notes: string | null;
  status: PaymentStatus;
  voucherId: number | null;
  reversalVoucherId: number | null;
  reversalReason: string | null;
  createdAt: string;
  allocations: PaymentAllocation[];
}
export interface PaymentSummary {
  id: number; paymentNumber: string; paymentDate: string; vendorId: number; vendorName: string;
  paymentMethod: PaymentMethod; amount: number; status: PaymentStatus; allocationCount: number;
}
export interface PaymentPage { content: PaymentSummary[]; totalPages: number; totalElements: number; number: number; size: number; }

export interface PayableBill {
  id: number;
  billNumber: string;
  billDate: string;
  dueDate: string | null;
  vendorId: number;
  vendorName: string;
  totalAmount: number;
  settledAmount: number;
  outstandingAmount: number;
}
export interface PayableDocument {
  documentType: "PURCHASE_BILL" | "EXPENSE"; id: number; documentNumber: string; documentDate: string;
  dueDate: string | null; vendorId: number; vendorName: string; totalAmount: number;
  settledAmount: number; outstandingAmount: number;
}
export interface SettlementHistory {
  paymentId: number; paymentNumber: string; paymentDate: string; paymentMethod: PaymentMethod;
  paymentAmount: number; allocatedAmount: number; status: PaymentStatus; referenceNumber: string | null;
  voucherId: number | null; reversalVoucherId: number | null;
}

export interface PaymentPayload {
  paymentDate: string;
  vendorId: number;
  paymentMethod: PaymentMethod;
  sourceLedgerId: number;
  amount: number;
  referenceNumber: string | null;
  notes: string | null;
  allocations: { documentType: string; documentId: number; allocatedAmount: number }[];
}

export const paymentService = {
  list(page = 0, search = "", sort = "paymentDate,desc", signal?: AbortSignal): Promise<PaymentPage> {
    const params = new URLSearchParams({ page: String(page), size: "50", sort, ...(search ? { search } : {}) });
    return apiClient.get<PaymentPage>(`${API.payments.list}?${params.toString()}`, { signal });
  },
  get(id: number, signal?: AbortSignal): Promise<Payment> {
    return apiClient.get<Payment>(API.payments.detail(id), { signal });
  },
  payableBills(vendorId: number, signal?: AbortSignal): Promise<PayableBill[]> {
    return apiClient.get<PayableBill[]>(`${API.payments.payableBills}?vendorId=${vendorId}`, { signal });
  },
  payableDocuments(vendorId: number, signal?: AbortSignal): Promise<PayableDocument[]> {
    return apiClient.get<PayableDocument[]>(`${API.payments.payableDocuments}?vendorId=${vendorId}`, { signal });
  },
  settlementHistory(documentType: string, documentId: number, signal?: AbortSignal): Promise<SettlementHistory[]> {
    return apiClient.get<SettlementHistory[]>(`${API.payments.settlementHistory}?documentType=${encodeURIComponent(documentType)}&documentId=${documentId}`, { signal });
  },
  create(payload: PaymentPayload): Promise<Payment> {
    return apiClient.post<Payment>(API.payments.create, payload);
  },
  update(id: number, payload: PaymentPayload): Promise<Payment> {
    return apiClient.put<Payment>(API.payments.update(id), payload);
  },
  post(id: number): Promise<Payment> {
    return apiClient.post<Payment>(API.payments.post(id), {});
  },
  void(id: number): Promise<Payment> {
    return apiClient.post<Payment>(API.payments.void(id), {});
  },
  reverse(id: number, reason: string, reversalDate: string): Promise<Payment> {
    return apiClient.post<Payment>(API.payments.reverse(id), { reason, reversalDate });
  },
};
