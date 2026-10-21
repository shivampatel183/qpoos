import { AlertCircle, CheckCircle, Info } from 'lucide-react';

export type AlertVariant = 'error' | 'success' | 'info';

interface AlertProps {
    variant?: AlertVariant;
    message: string;
}

const ICONS: Record<AlertVariant, React.ReactNode> = {
    error:   <AlertCircle size={15} />,
    success: <CheckCircle size={15} />,
    info:    <Info size={15} />,
};

export function Alert({ variant = 'error', message }: AlertProps) {
    return (
        <div role="alert" className={`alert alert--${variant}`}>
            {ICONS[variant]}
            <span>{message}</span>
        </div>
    );
}
