import React from "react";
import {
  Plus,
  Bookmark,
  Home,
  BarChart2,
  Grid,
  Book,
  CreditCard,
  DollarSign,
  Zap,
  Users,
  Clock,
  Box,
  Percent,
  Briefcase,
  CheckSquare,
  Activity,
} from "lucide-react";

export type PrimaryNavItem = {
  id: string;
  label: string;
  icon: React.ReactNode;
  isButton?: boolean;
  actionName?: string;
};

export type SecondarySubItem = {
  id: string;
  label: string;
  path: string;
};

export type SecondaryNavItem = {
  id: string;
  label: string;
  icon: React.ReactNode;
  path: string;
  isPremium?: boolean;
  children?: SecondarySubItem[];
};

export const primaryTopItems: PrimaryNavItem[] = [
  {
    id: "create",
    label: "Create",
    icon: <Plus size={20} strokeWidth={2.5} />,
    actionName: "CREATE_ACTION",
  },
  {
    id: "bookmarks",
    label: "Bookmarks",
    icon: <Bookmark size={22} strokeWidth={1.5} />,
    actionName: "GOTO_BOOKMARKS",
  },
  {
    id: "home",
    label: "Home",
    icon: <Home size={22} strokeWidth={1.5} />,
    actionName: "GOTO_HOME",
  },
  {
    id: "reports",
    label: "Reports",
    icon: <BarChart2 size={22} strokeWidth={1.5} />,
    actionName: "GOTO_REPORTS",
  },
  {
    id: "apps",
    label: "All apps",
    icon: <Grid size={22} strokeWidth={1.5} />,
    actionName: "TOGGLE_APPS",
  },
];

export const secondaryNavItems: SecondaryNavItem[] = [
  {
    id: "accounting",
    label: "Accounting",
    path: "/accounting",
    icon: <Book size={18} strokeWidth={2} />,
    children: [
      {
        id: "acc-dashboard",
        label: "Dashboard",
        path: "/accounting/dashboard",
      },
      {
        id: "acc-chart",
        label: "Chart of Accounts",
        path: "/accounting/chart",
      },
      {
        id: "acc-journal",
        label: "Journal Entries",
        path: "/accounting/journal",
      },
    ],
  },
  {
    id: "expenses",
    label: "Expenses",
    path: "/expenses",
    icon: <CreditCard size={18} strokeWidth={2} />,
    children: [
      { id: "exp-all", label: "Expenses", path: "/expenses/all" },
      { id: "exp-bills", label: "Bills", path: "/purchase-bills" },
      {
        id: "purchase-orders",
        label: "Purchase Orders",
        path: "/purchase-orders",
      },
      { id: "exp-vendors", label: "Vendors", path: "/expenses/vendors" },
    ],
  },
  {
    id: "sales",
    label: "Sales & Get Paid",
    path: "/sales",
    icon: <DollarSign size={18} strokeWidth={2} />,
    children: [
      { id: "sales-invoices", label: "Invoices", path: "/sales/invoices" },
      { id: "sales-payments", label: "Payments", path: "/sales/payments" },
    ],
  },
  {
    id: "customer",
    label: "Customer Hub",
    path: "/customer-hub",
    icon: <Zap size={18} strokeWidth={2} />,
    children: [
      {
        id: "customers-leads",
        label: "Customers & Leads",
        path: "/customers-hub/customers",
      },
    ],
  },
  {
    id: "team",
    label: "Team",
    path: "/team",
    icon: <Users size={18} strokeWidth={2} />,
  },
  {
    id: "time",
    label: "Time",
    path: "/time",
    icon: <Clock size={18} strokeWidth={2} />,
  },
  {
    id: "inventory",
    label: "Inventory",
    path: "/inventory",
    icon: <Box size={18} strokeWidth={2} />,
    children: [
      { id: "inv-products", label: "Products", path: "/inventory/products" },
    ],
  },
  {
    id: "salestax",
    label: "Sales Tax",
    path: "/sales-tax",
    icon: <Percent size={18} strokeWidth={2} />,
  },
  {
    id: "businesstax",
    label: "Business Tax",
    path: "/business-tax",
    icon: <Briefcase size={18} strokeWidth={2} />,
  },
  {
    id: "lending",
    label: "Lending",
    path: "/lending",
    icon: <DollarSign size={18} strokeWidth={2} />,
  },
  {
    id: "projects",
    label: "Projects",
    path: "/projects",
    icon: <CheckSquare size={18} strokeWidth={2} />,
    isPremium: true,
  },
  {
    id: "payroll",
    label: "Payroll",
    path: "/payroll",
    icon: <Users size={18} strokeWidth={2} />,
    isPremium: true,
  },
  {
    id: "marketing",
    label: "Marketing",
    path: "/marketing",
    icon: <Activity size={18} strokeWidth={2} />,
    isPremium: true,
  },
];
