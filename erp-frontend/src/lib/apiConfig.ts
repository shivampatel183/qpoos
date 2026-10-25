// ─── API Configuration ────────────────────────────────────────────────────────
//
// Central place for base URL and every endpoint path in the application.
// Import `API` from here — never hardcode URLs in components or services.
//
// Usage:
//   import { API } from '@/lib/apiConfig';
//   apiClient.post(API.auth.login, body);
// ─────────────────────────────────────────────────────────────────────────────

// Base URL — reads from the Vite env variable; falls back to /api for proxy setups.
export const BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ??
  "http://localhost:8080/api";

// ── All endpoint paths ────────────────────────────────────────────────────────

export const API = {
  // ── Auth ──────────────────────────────────────────────────────────────────
  auth: {
    login: "/auth/login",
    register: "/auth/register",
    logout: "/auth/logout",
    refreshToken: "/auth/refresh",
    me: "/auth/me",
    forgotPassword: "/auth/forgot-password",
    resetPassword: "/auth/reset-password",
  },

  // ── Users ─────────────────────────────────────────────────────────────────
  users: {
    list: "/users",
    create: "/users",
    detail: (id: string | number) => `/users/${id}`,
    update: (id: string | number) => `/users/${id}`,
    delete: (id: string | number) => `/users/${id}`,
  },

  // ── Inventory ─────────────────────────────────────────────────────────────
  inventory: {
    list: "/inventory",
    create: "/inventory",
    detail: (id: string | number) => `/inventory/${id}`,
    update: (id: string | number) => `/inventory/${id}`,
    delete: (id: string | number) => `/inventory/${id}`,
  },

  // ── Accounting ────────────────────────────────────────────────────────────
  accounting: {
    ledgers: "/accounting/ledgers",
    ledger: (id: string | number) => `/accounting/ledgers/${id}`,
    journals: "/accounting/journals",
    journal: (id: string | number) => `/accounting/journals/${id}`,
    accountGroups: "/accounting/account-groups",
    accountTypes: "/accounting/account-types",
    transactions: "/accounting/transactions",
    summary: "/accounting/summary",
  },

  vendors: {
    list: "/vendors",
    create: "/vendors",
    detail: (id: string | number) => `/vendors/${id}`,
    update: (id: string | number) => `/vendors/${id}`,
    delete: (id: string | number) => `/vendors/${id}`,
  },

  customers: {
    list: "/customers",
    create: "/customers",
    detail: (id: string | number) => `/customers/${id}`,
    update: (id: string | number) => `/customers/${id}`,
    delete: (id: string | number) => `/customers/${id}`,
  },

  products: {
    list: "/products",
    create: "/products",
    detail: (id: string) => `/products/${id}`,
    update: (id: string) => `/products/${id}`,
    delete: (id: string) => `/products/${id}`,
  },

  transactions: {
    list: "/transactions",
    create: "/transactions",
    detail: (id: string | number) => `/transactions/${id}`,
    update: (id: string | number) => `/transactions/${id}`,
    delete: (id: string | number) => `/transactions/${id}`,
    post: (id: string | number) => `/transactions/${id}/post`,
  },
  purchaseOrders: {
    list: "/purchase-orders",
    create: "/purchase-orders",
    detail: (id: string | number) => `/purchase-orders/${id}`,
    update: (id: string | number) => `/purchase-orders/${id}`,
    submit: (id: string | number) => `/purchase-orders/${id}/submit`,
    approve: (id: string | number) => `/purchase-orders/${id}/approve`,
    receive: (id: string | number) => `/purchase-orders/${id}/receive`,
    close: (id: string | number) => `/purchase-orders/${id}/close`,
    cancel: (id: string | number) => `/purchase-orders/${id}/cancel`,
  },
  purchaseBills: {
    list: "/purchase-bills",
    create: "/purchase-bills",
    detail: (id: string | number) => `/purchase-bills/${id}`,
    update: (id: string | number) => `/purchase-bills/${id}`,
    post: (id: string | number) => `/purchase-bills/${id}/post`,
    cancel: (id: string | number) => `/purchase-bills/${id}/cancel`,
  },
  paymentTerms: {
    list: "/payment-terms",
  },
  // ── HR ────────────────────────────────────────────────────────────────────
  hr: {
    employees: "/hr/employees",
    attendance: "/hr/attendance",
    payroll: "/hr/payroll",
  },

  // ── Company ───────────────────────────────────────────────────────────────
  company: {
    list: "/company",
    create: "/company",
    switchCompany: (id: string | number) => `/company/switch-company/${id}`,
  },

  // ── Reports ───────────────────────────────────────────────────────────────
  reports: {
    sales: "/reports/sales",
    purchase: "/reports/purchase",
    profitLoss: "/reports/profit-loss",
    balanceSheet: "/reports/balance-sheet",
  },
} as const;
