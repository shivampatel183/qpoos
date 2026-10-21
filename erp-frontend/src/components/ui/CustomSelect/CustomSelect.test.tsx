import { fireEvent, render, screen } from '@testing-library/react';
import { CustomSelect } from './CustomSelect';

describe('CustomSelect', () => {
    it('opens, exposes themed listbox options, and selects a value', () => {
        const onChange = vi.fn();

        render(
            <CustomSelect
                value=""
                onChange={onChange}
                options={[
                    { value: 'cash', label: 'Cash' },
                    { value: 'bank', label: 'Bank' },
                ]}
                placeholder="Select account type"
                ariaLabel="Account Type"
                maxMenuHeight={160}
            />,
        );

        const trigger = screen.getByRole('combobox', { name: 'Account Type' });
        expect(trigger).toHaveAttribute('aria-expanded', 'false');

        fireEvent.click(trigger);

        expect(trigger).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('listbox', { name: 'Account Type' }))
            .toHaveStyle({ maxHeight: '160px' });

        fireEvent.click(screen.getByRole('option', { name: 'Bank' }));

        expect(onChange).toHaveBeenCalledWith('bank');
        expect(trigger).toHaveAttribute('aria-expanded', 'false');
    });

    it('supports arrow-key navigation and selection', () => {
        const onChange = vi.fn();

        render(
            <CustomSelect
                value=""
                onChange={onChange}
                options={[
                    { value: 'cash', label: 'Cash' },
                    { value: 'bank', label: 'Bank' },
                ]}
                ariaLabel="Account Type"
            />,
        );

        const trigger = screen.getByRole('combobox', { name: 'Account Type' });
        fireEvent.keyDown(trigger, { key: 'ArrowDown' });
        fireEvent.keyDown(trigger, { key: 'ArrowDown' });
        fireEvent.keyDown(trigger, { key: 'Enter' });

        expect(onChange).toHaveBeenCalledWith('bank');
    });
});
