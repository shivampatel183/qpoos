import { useCallback, useEffect, useMemo, useState } from 'react';
import { Edit2, Plus, Trash2 } from 'lucide-react';
import { ProductFormModal } from '@/components/products';
import { DataTable } from '@/components/ui/DataTable';
import type { DataTableColumn } from '@/components/ui/DataTable';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import { useCompany } from '@/context/CompanyContext';
import { ApiError } from '@/lib/apiClient';
import { productService, type Product, type ProductType } from '@/services/productService';
import './ProductPage.css';

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof ApiError ? error.message : fallback;
}

function formatPrice(value: string | null): string {
    if (!value) return '—';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(Number(value));
}

const TYPE_LABELS: Record<ProductType, string> = {
    INVENTORY: 'Inventory',
    NON_INVENTORY: 'Non-Inventory',
    SERVICE: 'Service',
};

const TYPE_BADGE_CLASS: Record<ProductType, string> = {
    INVENTORY: 'product-page__badge product-page__badge--inventory',
    NON_INVENTORY: 'product-page__badge product-page__badge--non-inventory',
    SERVICE: 'product-page__badge product-page__badge--service',
};

export function ProductPage() {
    const { currentCompany } = useCompany();
    const messagePopup = useMessagePopup();
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null);
    const [editingProduct, setEditingProduct] = useState<Product | null>(null);

    const loadData = useCallback(async (signal?: AbortSignal) => {
        setLoading(true);
        try {
            setProducts(await productService.list(signal));
        } catch (error) {
            if (!signal?.aborted) {
                messagePopup.error(getErrorMessage(error, 'Failed to load products.'));
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
        setEditingProduct(null);
        setModalMode('create');
    }

    async function openEditModal(product: Product) {
        try {
            const details = await productService.get(product.id);
            setEditingProduct(details);
            setModalMode('edit');
        } catch (error) {
            messagePopup.error(getErrorMessage(error, 'Failed to load product details.'));
        }
    }

    function closeModal() {
        setModalMode(null);
        setEditingProduct(null);
    }

    function handleProductSaved(saved: Product) {
        setProducts((current) => {
            const exists = current.some((p) => p.id === saved.id);
            return exists
                ? current.map((p) => (p.id === saved.id ? saved : p))
                : [...current, saved];
        });
    }

    async function handleDelete(product: Product) {
        if (!window.confirm(`Delete "${product.name}"? This cannot be undone.`)) return;
        try {
            await productService.delete(product.id);
            setProducts((current) => current.filter((p) => p.id !== product.id));
            messagePopup.success('Product deleted successfully.');
        } catch (error) {
            messagePopup.error(getErrorMessage(error, 'Failed to delete product.'));
        }
    }

    const columns = useMemo<DataTableColumn<Product>[]>(() => [
        {
            key: 'name',
            header: 'Product',
            render: (product) => (
                <div className="product-page__name">
                    <strong>{product.name}</strong>
                    {product.sku && <span>SKU: {product.sku}</span>}
                </div>
            ),
        },
        {
            key: 'type',
            header: 'Type',
            width: '150px',
            render: (product) => (
                <span className={TYPE_BADGE_CLASS[product.type]}>
                    {TYPE_LABELS[product.type]}
                </span>
            ),
        },
        {
            key: 'salePrice',
            header: 'Sale Price',
            align: 'right',
            width: '130px',
            render: (product) => (
                <span className="product-page__price">{formatPrice(product.salePrice)}</span>
            ),
        },
        {
            key: 'purchaseCost',
            header: 'Purchase Cost',
            align: 'right',
            width: '140px',
            hideOnMobile: true,
            render: (product) => (
                <span className="product-page__price">{formatPrice(product.purchaseCost)}</span>
            ),
        },
        {
            key: 'quantityOnHand',
            header: 'On Hand',
            align: 'right',
            width: '110px',
            hideOnMobile: true,
            render: (product) =>
                product.quantityOnHand != null
                    ? <span className="product-page__price">{product.quantityOnHand}</span>
                    : '—',
        },
        {
            key: 'hsnCode',
            header: 'HSN Code',
            width: '120px',
            hideOnMobile: true,
            render: (product) => product.hsnCode ?? '—',
        },
    ], []);

    return (
        <div className="product-page">
            <DataTable
                data={products}
                columns={columns}
                rowKey="id"
                title="Products"
                loading={loading}
                loadingText="Loading products..."
                onRetry={() => loadData()}
                searchable
                searchPlaceholder="Search products..."
                defaultSort={{ key: 'name', direction: 'asc' }}
                pageSize={25}
                density="compact"
                primaryAction={{
                    label: 'New Product',
                    icon: Plus,
                    onClick: openCreateModal,
                }}
                actions={[]}
                moreActions={[
                    {
                        label: 'Edit product',
                        icon: Edit2,
                        onClick: openEditModal,
                    },
                    {
                        label: 'Delete product',
                        icon: Trash2,
                        danger: true,
                        onClick: handleDelete,
                    },
                ]}
                emptyTitle="No products found"
                emptyMessage="Add your first product or service to get started."
                emptyAction={{
                    label: 'New Product',
                    icon: Plus,
                    onClick: openCreateModal,
                }}
            />

            {modalMode && (
                <ProductFormModal
                    product={modalMode === 'edit' ? (editingProduct ?? undefined) : undefined}
                    onClose={closeModal}
                    onSaved={handleProductSaved}
                />
            )}
        </div>
    );
}
