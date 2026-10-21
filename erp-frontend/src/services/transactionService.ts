import { apiClient } from "@/lib/apiClient";
import { API } from "@/lib/apiConfig";

export type TransactionType =
  | "PURCHASE_BILL"
  | "PURCHASE_ORDER"
  | "PURCHASE_RETURN"
  | "EXPENSE"
  | "PAYMENT";

export type PaymentStatus =
  | "UNPAID"
  | "PARTIALLY_PAID"
  | "PAID"
  | "OVERDUE"
  | "CANCELLED";

export type TransactionLineType = "ITEM" | "ACCOUNT";

export interface TransactionListItem {
  id: number;
  transactionDate: string;
  transactionType: TransactionType;
  billNo: string | null;
  vendorId: number | null;
  vendorName: string | null;
  dueDate: string | null;
  paymentDate: string | null;
  totalAmount: number;
  paymentStatus: PaymentStatus;
}

export interface TransactionLine {
  id?: number;
  lineNo: number;
  lineType: TransactionLineType;
  itemId: number | null;
  accountId: number | null;
  description: string | null;
  quantity: number;
  unit: string | null;
  rate: number;
  discount: number;
  taxRate: number;
  taxAmount: number;
  amount: number;
}

export interface Transaction extends TransactionListItem {
  companyId: string;
  referenceNo: string | null;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  roundOff: number;
  paymentDate: string | null;
  notes: string | null;
  attachments: string | null;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
  lines: TransactionLine[];
}

export interface TransactionLinePayload {
  lineNo: number;
  lineType: TransactionLineType;
  itemId: number | null;
  accountId: number | null;
  description: string | null;
  quantity: number;
  unit: string | null;
  rate: number;
  discount: number;
  taxRate: number;
  taxAmount: number;
  amount: number;
}

export interface TransactionPayload {
  transactionDate: string;
  transactionType: TransactionType;
  billNo: string | null;
  vendorId: number | null;
  dueDate: string | null;
  referenceNo: string | null;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  roundOff: number;
  totalAmount: number;
  paymentDate: string | null;
  paymentStatus: PaymentStatus;
  notes: string | null;
  attachments: string | null;
  lines: TransactionLinePayload[];
}

export const transactionService = {
  list(signal?: AbortSignal): Promise<TransactionListItem[]> {
    return apiClient.get<TransactionListItem[]>(API.transactions.list, {
      signal,
    });
  },

  get(id: number, signal?: AbortSignal): Promise<Transaction> {
    return apiClient.get<Transaction>(API.transactions.detail(id), { signal });
  },

  create(payload: TransactionPayload): Promise<Transaction> {
    return apiClient.post<Transaction>(API.transactions.create, payload);
  },

  update(id: number, payload: TransactionPayload): Promise<Transaction> {
    return apiClient.put<Transaction>(API.transactions.update(id), payload);
  },

  delete(id: number): Promise<void> {
    return apiClient.delete<void>(API.transactions.delete(id));
  },
};
