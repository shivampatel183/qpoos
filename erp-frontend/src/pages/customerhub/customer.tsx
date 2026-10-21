import { useCallback, useEffect, useMemo, useState } from "react";
import { Edit2, Plus, Trash2 } from "lucide-react";
import { CustomerFormModal } from "@/components/customers";
import { DataTable } from "@/components/ui/DataTable";
import type { DataTableColumn } from "@/components/ui/DataTable";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { useCompany } from "@/context/CompanyContext";
import { ApiError } from "@/lib/apiClient";
import { customerService, type Customer } from "@/services/customerService";
import "./customer.css";

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatBalance(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function contactText(customer: Customer): string {
  return customer.email || customer.mobileNo || "-";
}

export function CustomerPage() {
  const { currentCompany } = useCompany();
  const messagePopup = useMessagePopup();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);

  const loadData = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      try {
        setCustomers(await customerService.list(signal));
      } catch (error) {
        if (!signal?.aborted) {
          messagePopup.error(
            getErrorMessage(error, "Failed to load customers."),
          );
        }
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [messagePopup],
  );

  useEffect(() => {
    const controller = new AbortController();
    void loadData(controller.signal);
    return () => controller.abort();
  }, [currentCompany?.id, loadData]);

  function openCreateModal() {
    setEditingCustomer(null);
    setModalMode("create");
  }

  async function openEditModal(customer: Customer) {
    try {
      const customerDetails = await customerService.get(customer.id);
      setEditingCustomer(customerDetails);
      setModalMode("edit");
    } catch (error) {
      messagePopup.error(
        getErrorMessage(error, "Failed to load customer details."),
      );
    }
  }

  function closeModal() {
    setModalMode(null);
    setEditingCustomer(null);
  }

  function handleCustomerSaved(savedCustomer: Customer) {
    setCustomers((current) => {
      const exists = current.some(
        (customer) => customer.id === savedCustomer.id,
      );
      return exists
        ? current.map((customer) =>
            customer.id === savedCustomer.id ? savedCustomer : customer,
          )
        : [...current, savedCustomer];
    });
  }

  async function handleDelete(customer: Customer) {
    if (!window.confirm(`Delete ${customer.displayName}?`)) return;
    try {
      await customerService.delete(customer.id);
      setCustomers((current) =>
        current.filter((item) => item.id !== customer.id),
      );
      messagePopup.success("Customer deleted successfully.");
    } catch (error) {
      messagePopup.error(getErrorMessage(error, "Failed to delete customer."));
    }
  }

  const columns = useMemo<DataTableColumn<Customer>[]>(
    () => [
      {
        key: "displayName",
        header: "Customer",
        render: (customer) => (
          <div className="customer-page__name">
            <strong>{customer.displayName}</strong>
            {customer.companyName && <span>{customer.companyName}</span>}
          </div>
        ),
      },
      {
        key: "email",
        header: "Contact",
        render: (customer) => (
          <div className="customer-page__contact">
            <span>{contactText(customer)}</span>
            {customer.email && customer.mobileNo && (
              <small>{customer.mobileNo}</small>
            )}
          </div>
        ),
      },
      {
        key: "gstNo",
        header: "GST No",
        hideOnMobile: true,
        render: (customer) => customer.gstNo || "-",
      },
      {
        key: "city",
        header: "Location",
        hideOnMobile: true,
        render: (customer) =>
          [customer.city, customer.state].filter(Boolean).join(", ") || "-",
      },
      {
        key: "openingBalance",
        header: "Opening Balance",
        align: "right",
        width: "160px",
        render: (customer) => (
          <span className="customer-page__balance">
            {formatBalance(customer.openingBalance)}
          </span>
        ),
      },
    ],
    [],
  );

  return (
    <div className="customer-page">
      <DataTable
        data={customers}
        columns={columns}
        rowKey="id"
        title="Customers & Leads"
        loading={loading}
        loadingText="Loading customers..."
        onRetry={() => loadData()}
        searchable
        searchPlaceholder="Search customers..."
        defaultSort={{ key: "displayName", direction: "asc" }}
        pageSize={25}
        density="compact"
        primaryAction={{
          label: "New Customer",
          icon: Plus,
          onClick: openCreateModal,
        }}
        actions={[]}
        moreActions={[
          {
            label: "Edit customer",
            icon: Edit2,
            onClick: openEditModal,
          },
          {
            label: "Delete customer",
            icon: Trash2,
            danger: true,
            onClick: handleDelete,
          },
        ]}
        emptyTitle="No customers found"
        emptyMessage="Create your first customer for this company."
        emptyAction={{
          label: "New Customer",
          icon: Plus,
          onClick: openCreateModal,
        }}
      />

      {modalMode && (
        <CustomerFormModal
          customer={
            modalMode === "edit" ? (editingCustomer ?? undefined) : undefined
          }
          onClose={closeModal}
          onSaved={handleCustomerSaved}
        />
      )}
    </div>
  );
}
