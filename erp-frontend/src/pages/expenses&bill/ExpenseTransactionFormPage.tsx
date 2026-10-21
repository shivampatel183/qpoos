import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { TransactionForm } from '@/components/transactions';
import { Spinner } from '@/components/ui/Spinner';
import { useMessagePopup } from '@/components/ui/MessagePopup';
import { ApiError } from '@/lib/apiClient';
import { transactionService, type Transaction } from '@/services/transactionService';
import '../purchase-orders/PurchaseOrderPage.css';
import './ExpenseTransactionFormPage.css';

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof ApiError ? error.message : fallback;
}

export function ExpenseTransactionFormPage() {
    const navigate = useNavigate();
    const { transactionId } = useParams();
    const messagePopup = useMessagePopup();
    const [transaction, setTransaction] = useState<Transaction | null>(null);
    const [loading, setLoading] = useState(Boolean(transactionId));

    const goBack = useCallback(() => {
        navigate('/expenses/all');
    }, [navigate]);

    useEffect(() => {
        if (!transactionId) return undefined;

        const id = Number(transactionId);
        if (!Number.isFinite(id)) {
            messagePopup.error('Invalid transaction id.');
            goBack();
            return undefined;
        }

        const controller = new AbortController();
        async function loadTransaction() {
            setLoading(true);
            try {
                setTransaction(await transactionService.get(id, controller.signal));
            } catch (error) {
                if (!controller.signal.aborted) {
                    messagePopup.error(getErrorMessage(error, 'Failed to load transaction.'));
                    goBack();
                }
            } finally {
                if (!controller.signal.aborted) setLoading(false);
            }
        }

        void loadTransaction();
        return () => controller.abort();
    }, [goBack, messagePopup, transactionId]);

    return (
        <div className="expense-transaction-form-page">
            {loading ? (
                <div className="expense-transaction-form-page__loading">
                    <Spinner size={22} />
                    <span>Loading transaction...</span>
                </div>
            ) : (
                <>
                    <button
                        type="button"
                        className="purchase-order-page__back"
                        onClick={goBack}
                    >
                        <ArrowLeft size={16} /> Back to expenses
                    </button>
                    <TransactionForm
                        transaction={transaction ?? undefined}
                        onCancel={goBack}
                        onSaved={goBack}
                    />
                </>
            )}
        </div>
    );
}
