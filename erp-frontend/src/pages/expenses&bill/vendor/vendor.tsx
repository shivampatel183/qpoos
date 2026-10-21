import { useCallback, useEffect, useMemo, useState } from 'react';
import { Edit2, Plus, Trash2 } from 'lucide-react';
import { VendorFormModal } from '@/components/vendors';
import { DataTable } from '@/components/ui/DataTable';
import type { DataTableColumn } from '@/components/ui/DataTable';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import { useCompany } from '@/context/CompanyContext';
import { ApiError } from '@/lib/apiClient';
import { vendorService, type Vendor } from '@/services/vendorService';
import './vendor.css';

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof ApiError ? error.message : fallback;
}

function formatBalance(value: number): string {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(value);
}

function contactText(vendor: Vendor): string {
    return vendor.email || vendor.mobileNo || '-';
}

export function VendorPage() {
    const { currentCompany } = useCompany();
    const messagePopup = useMessagePopup();
    const [vendors, setVendors] = useState<Vendor[]>([]);
    const [loading, setLoading] = useState(true);
    const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null);
    const [editingVendor, setEditingVendor] = useState<Vendor | null>(null);

    const loadData = useCallback(async (signal?: AbortSignal) => {
        setLoading(true);
        try {
            setVendors(await vendorService.list(signal));
        } catch (error) {
            if (!signal?.aborted) {
                messagePopup.error(getErrorMessage(error, 'Failed to load vendors.'));
            }
        } finally {
            if (!signal?.aborted) setLoading(false);
        }
    }, [messagePopup]);

    useEffect(() => {
        const controller = new AbortController();
        void loadData(controller.signal);
        return () => controller.abort();
    }, [currentCompany?.id, loadData]);

    function openCreateModal() {
        setEditingVendor(null);
        setModalMode('create');
    }

    async function openEditModal(vendor: Vendor) {
        try {
            const vendorDetails = await vendorService.get(vendor.id);
            setEditingVendor(vendorDetails);
            setModalMode('edit');
        } catch (error) {
            messagePopup.error(getErrorMessage(error, 'Failed to load vendor details.'));
        }
    }

    function closeModal() {
        setModalMode(null);
        setEditingVendor(null);
    }

    function handleVendorSaved(savedVendor: Vendor) {
        setVendors((current) => {
            const exists = current.some((vendor) => vendor.id === savedVendor.id);
            return exists
                ? current.map((vendor) => vendor.id === savedVendor.id ? savedVendor : vendor)
                : [...current, savedVendor];
        });
    }

    async function handleDelete(vendor: Vendor) {
        if (!window.confirm(`Delete ${vendor.displayName}?`)) return;
        try {
            await vendorService.delete(vendor.id);
            setVendors((current) => current.filter((item) => item.id !== vendor.id));
            messagePopup.success('Vendor deleted successfully.');
        } catch (error) {
            messagePopup.error(getErrorMessage(error, 'Failed to delete vendor.'));
        }
    }

    const columns = useMemo<DataTableColumn<Vendor>[]>(() => [
        {
            key: 'displayName',
            header: 'Vendor',
            render: (vendor) => (
                <div className="vendor-page__name">
                    <strong>{vendor.displayName}</strong>
                    {vendor.companyName && <span>{vendor.companyName}</span>}
                </div>
            ),
        },
        {
            key: 'email',
            header: 'Contact',
            render: (vendor) => (
                <div className="vendor-page__contact">
                    <span>{contactText(vendor)}</span>
                    {vendor.email && vendor.mobileNo && <small>{vendor.mobileNo}</small>}
                </div>
            ),
        },
        {
            key: 'gstNo',
            header: 'GST No',
            hideOnMobile: true,
            render: (vendor) => vendor.gstNo || '-',
        },
        {
            key: 'city',
            header: 'Location',
            hideOnMobile: true,
            render: (vendor) => [vendor.city, vendor.state].filter(Boolean).join(', ') || '-',
        },
        {
            key: 'openingBalance',
            header: 'Opening Balance',
            align: 'right',
            width: '160px',
            render: (vendor) => (
                <span className="vendor-page__balance">
                    {formatBalance(vendor.openingBalance)}
                </span>
            ),
        },
    ], []);

    return (
        <div className="vendor-page">
            <DataTable
                data={vendors}
                columns={columns}
                rowKey="id"
                title="Vendors"
                loading={loading}
                loadingText="Loading vendors..."
                onRetry={() => loadData()}
                searchable
                searchPlaceholder="Search vendors..."
                defaultSort={{ key: 'displayName', direction: 'asc' }}
                pageSize={25}
                density="compact"
                primaryAction={{
                    label: 'New Vendor',
                    icon: Plus,
                    onClick: openCreateModal,
                }}
                actions={[]}
                moreActions={[
                    {
                        label: 'Edit vendor',
                        icon: Edit2,
                        onClick: openEditModal,
                    },
                    {
                        label: 'Delete vendor',
                        icon: Trash2,
                        danger: true,
                        onClick: handleDelete,
                    },
                ]}
                emptyTitle="No vendors found"
                emptyMessage="Create your first vendor for this company."
                emptyAction={{
                    label: 'New Vendor',
                    icon: Plus,
                    onClick: openCreateModal,
                }}
            />

            {modalMode && (
                <VendorFormModal
                    vendor={modalMode === 'edit' ? (editingVendor ?? undefined) : undefined}
                    onClose={closeModal}
                    onSaved={handleVendorSaved}
                />
            )}
        </div>
    );
}
