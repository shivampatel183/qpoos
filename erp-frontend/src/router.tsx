import { lazy, Suspense, type ReactNode } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { Layout } from "./layout";
import { SessionRenewalManager } from "@/components/SessionRenewalManager";
import { AuthGuard } from "@/components/guards/AuthGuard";
import { CompanyGuard } from "@/components/guards/CompanyGuard";
import { GuestGuard } from "@/components/guards/GuestGuard";
import { Spinner } from "@/components/ui/Spinner";

const LoginPage = lazy(() =>
  import("./pages/auth/LoginPage").then((module) => ({
    default: module.LoginPage,
  })),
);
const RegisterPage = lazy(() =>
  import("./pages/auth/RegisterPage").then((module) => ({
    default: module.RegisterPage,
  })),
);
const CompanyPage = lazy(() =>
  import("./pages/company/CompanyPage").then((module) => ({
    default: module.CompanyPage,
  })),
);
const HomePage = lazy(() =>
  import("./pages/home/HomePage").then((module) => ({
    default: module.HomePage,
  })),
);
const ChartOfAccountsPage = lazy(() =>
  import("./pages/accounting/chartofaccount/ChartOfAccountsPage").then(
    (module) => ({
      default: module.ChartOfAccountsPage,
    }),
  ),
);
const VendorPage = lazy(() =>
  import("./pages/expenses&bill/vendor/vendor").then((module) => ({
    default: module.VendorPage,
  })),
);
const ExpenseTransactionsPage = lazy(() =>
  import("./pages/expenses&bill/allexpenses/ExpenseTransactionsPage").then(
    (module) => ({
      default: module.ExpenseTransactionsPage,
    }),
  ),
);
const ExpenseTransactionFormPage = lazy(() =>
  import("./pages/expenses&bill/ExpenseTransactionFormPage").then((module) => ({
    default: module.ExpenseTransactionFormPage,
  })),
);
const CustomerPage = lazy(() =>
  import("./pages/customerhub/customer").then((module) => ({
    default: module.CustomerPage,
  })),
);
const ProductPage = lazy(() =>
  import("./pages/inventory/ProductPage").then((module) => ({
    default: module.ProductPage,
  })),
);
const PurchaseOrderListPage = lazy(() =>
  import("./pages/purchase-orders/PurchaseOrderListPage").then((module) => ({
    default: module.PurchaseOrderListPage,
  })),
);
const PurchaseOrderFormPage = lazy(() =>
  import("./pages/purchase-orders/PurchaseOrderFormPage").then((module) => ({
    default: module.PurchaseOrderFormPage,
  })),
);
const PurchaseBillListPage = lazy(() =>
  import("./pages/purchase-bills/PurchaseBillListPage").then((module) => ({
    default: module.PurchaseBillListPage,
  })),
);
const PurchaseBillFormPage = lazy(() =>
  import("./pages/purchase-bills/PurchaseBillFormPage").then((module) => ({
    default: module.PurchaseBillFormPage,
  })),
);

function PageLoader() {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "100vh",
      }}
    >
      <Spinner size={24} />
    </div>
  );
}

function page(element: ReactNode) {
  return <Suspense fallback={<PageLoader />}>{element}</Suspense>;
}

export const router = createBrowserRouter([
  {
    element: <SessionRenewalManager />,
    children: [
      {
        element: <GuestGuard />,
        children: [
          { path: "/login", element: page(<LoginPage />) },
          { path: "/register", element: page(<RegisterPage />) },
        ],
      },
      {
        element: <AuthGuard />,
        children: [
          { path: "/company-selection", element: page(<CompanyPage />) },
          {
            element: <CompanyGuard />,
            children: [
              {
                path: "/",
                element: <Layout />,
                children: [
                  { index: true, element: page(<HomePage />) },
                  {
                    path: "accounting/chart",
                    element: page(<ChartOfAccountsPage />),
                  },
                  {
                    path: "expenses/all",
                    element: page(<ExpenseTransactionsPage />),
                  },
                  {
                    path: "expenses/new",
                    element: page(<ExpenseTransactionFormPage />),
                  },
                  {
                    path: "expenses/:transactionId/edit",
                    element: page(<ExpenseTransactionFormPage />),
                  },
                  { path: "expenses/vendors", element: page(<VendorPage />) },
                  {
                    path: "customers-hub/customers",
                    element: page(<CustomerPage />),
                  },
                  {
                    path: "inventory/products",
                    element: page(<ProductPage />),
                  },
                  {
                    path: "purchase-orders",
                    element: page(<PurchaseOrderListPage />),
                  },
                  {
                    path: "purchase-orders/new",
                    element: page(<PurchaseOrderFormPage />),
                  },
                  {
                    path: "purchase-orders/:orderId/edit",
                    element: page(<PurchaseOrderFormPage />),
                  },
                  {
                    path: "purchase-orders/:orderId",
                    element: page(<PurchaseOrderFormPage />),
                  },
                  {
                    path: "purchase-bills",
                    element: page(<PurchaseBillListPage />),
                  },
                  {
                    path: "purchase-bills/new",
                    element: page(<PurchaseBillFormPage />),
                  },
                  {
                    path: "purchase-bills/:billId/edit",
                    element: page(<PurchaseBillFormPage />),
                  },
                  {
                    path: "purchase-bills/:billId",
                    element: page(<PurchaseBillFormPage />),
                  },
                  {
                    path: "expenses/bills",
                    element: <Navigate to="/purchase-bills" replace />,
                  },
                ],
              },
            ],
          },
        ],
      },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
]);
