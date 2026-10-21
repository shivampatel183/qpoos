interface SpinnerProps {
    /** Use inside dark/colored backgrounds (e.g. submit buttons). */
    white?: boolean;
    size?: number;
}

export function Spinner({ white = false, size = 16 }: SpinnerProps) {
    return (
        <span
            role="status"
            aria-label="Loading"
            className={`spinner${white ? ' spinner--white' : ''}`}
            style={{ width: size, height: size }}
        />
    );
}
