import { fireEvent, render, screen } from '@testing-library/react';
import { DataTable } from './DataTable';

vi.mock('@/components/ui/MessagePopup', () => ({
    useMessagePopup: () => ({
        error: vi.fn(),
    }),
}));

type AccountRow = {
    id: number;
    name: string;
    type: string;
};

const rows: AccountRow[] = [
    { id: 1, name: 'Counter Cash', type: 'Cash' },
    { id: 2, name: 'Main Bank', type: 'Bank' },
];

describe('DataTable filters', () => {
    it('generates dropdown options, filters rows, and clears active filters', () => {
        render(
            <DataTable
                data={rows}
                columns={[
                    { key: 'name', header: 'Name' },
                    { key: 'type', header: 'Type' },
                ]}
                rowKey="id"
                filters={[
                    { key: 'type', label: 'Account Type' },
                ]}
                pagination={false}
            />,
        );

        const filter = screen.getByRole('combobox', { name: 'Account Type' });
        fireEvent.click(filter);
        expect(screen.getByRole('option', { name: 'All Account Type' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Bank' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Cash' })).toBeInTheDocument();

        fireEvent.click(screen.getByRole('option', { name: 'Cash' }));
        expect(screen.getByText('Counter Cash')).toBeInTheDocument();
        expect(screen.queryByText('Main Bank')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }));
        expect(screen.getByText('Counter Cash')).toBeInTheDocument();
        expect(screen.getByText('Main Bank')).toBeInTheDocument();
    });
});
