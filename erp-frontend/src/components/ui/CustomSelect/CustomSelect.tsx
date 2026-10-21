import {
    useEffect,
    useId,
    useLayoutEffect,
    useMemo,
    useRef,
    useState,
    type CSSProperties,
    type KeyboardEvent,
    type ReactNode,
} from 'react';
import { Check, ChevronDown } from 'lucide-react';
import { createPortal } from 'react-dom';
import './CustomSelect.css';

export type CustomSelectOption = {
    value: string;
    label: string;
    disabled?: boolean;
};

export type CustomSelectGroup = {
    label: string;
    options: CustomSelectOption[];
};

export type CustomSelectProps = {
    value: string;
    onChange: (value: string) => void;
    options?: CustomSelectOption[];
    groups?: CustomSelectGroup[];
    placeholder?: string;
    ariaLabel?: string;
    ariaLabelledBy?: string;
    name?: string;
    disabled?: boolean;
    className?: string;
    maxMenuHeight?: number;
    size?: 'normal' | 'compact';
    bare?: boolean;
    leadingIcon?: ReactNode;
};

type MenuPosition = {
    top: number;
    left: number;
    width: number;
    maxHeight: number;
};

const VIEWPORT_MARGIN = 8;
const MENU_GAP = 6;
const OPTION_HEIGHT = 36;

export function CustomSelect({
    value,
    onChange,
    options = [],
    groups = [],
    placeholder = 'Select an option',
    ariaLabel,
    ariaLabelledBy,
    name,
    disabled = false,
    className,
    maxMenuHeight = 240,
    size = 'normal',
    bare = false,
    leadingIcon,
}: CustomSelectProps) {
    const generatedId = useId();
    const listboxId = `custom-select-${generatedId}`;
    const triggerRef = useRef<HTMLButtonElement>(null);
    const menuRef = useRef<HTMLDivElement>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const [menuPosition, setMenuPosition] = useState<MenuPosition | null>(null);

    const optionGroups = useMemo(
        () => [
            ...(options.length > 0 ? [{ label: '', options }] : []),
            ...groups,
        ],
        [groups, options],
    );
    const flatOptions = useMemo(
        () => optionGroups.flatMap((group) => group.options),
        [optionGroups],
    );
    const selectedOption = flatOptions.find((option) => option.value === value);

    function firstEnabledIndex(): number {
        return flatOptions.findIndex((option) => !option.disabled);
    }

    function lastEnabledIndex(): number {
        return flatOptions.findLastIndex((option) => !option.disabled);
    }

    function nextEnabledIndex(start: number, direction: 1 | -1): number {
        if (flatOptions.length === 0) return -1;

        for (let offset = 1; offset <= flatOptions.length; offset += 1) {
            const index = (start + direction * offset + flatOptions.length) % flatOptions.length;
            if (!flatOptions[index].disabled) return index;
        }

        return -1;
    }

    function openMenu(preferredIndex?: number) {
        if (disabled) return;
        const selectedIndex = flatOptions.findIndex((option) => option.value === value);
        setActiveIndex(
            preferredIndex
            ?? (selectedIndex >= 0 && !flatOptions[selectedIndex].disabled
                ? selectedIndex
                : firstEnabledIndex()),
        );
        setIsOpen(true);
    }

    function selectOption(option: CustomSelectOption) {
        if (option.disabled) return;
        onChange(option.value);
        setIsOpen(false);
        triggerRef.current?.focus();
    }

    useLayoutEffect(() => {
        if (!isOpen) {
            setMenuPosition(null);
            return undefined;
        }

        function positionMenu() {
            const trigger = triggerRef.current;
            if (!trigger) return;

            const rect = trigger.getBoundingClientRect();
            const expectedHeight = Math.min(
                maxMenuHeight,
                flatOptions.length * OPTION_HEIGHT + optionGroups.length * 28 + 8,
            );
            const availableBelow = window.innerHeight - rect.bottom - MENU_GAP - VIEWPORT_MARGIN;
            const availableAbove = rect.top - MENU_GAP - VIEWPORT_MARGIN;
            const openBelow = availableBelow >= Math.min(expectedHeight, maxMenuHeight)
                || availableBelow >= availableAbove;
            const availableHeight = Math.max(
                OPTION_HEIGHT + 8,
                openBelow ? availableBelow : availableAbove,
            );
            const resolvedMaxHeight = Math.min(maxMenuHeight, availableHeight);

            setMenuPosition({
                top: openBelow
                    ? rect.bottom + MENU_GAP
                    : Math.max(VIEWPORT_MARGIN, rect.top - MENU_GAP - Math.min(expectedHeight, resolvedMaxHeight)),
                left: Math.min(
                    Math.max(VIEWPORT_MARGIN, rect.left),
                    Math.max(VIEWPORT_MARGIN, window.innerWidth - rect.width - VIEWPORT_MARGIN),
                ),
                width: rect.width,
                maxHeight: resolvedMaxHeight,
            });
        }

        positionMenu();
        window.addEventListener('resize', positionMenu);
        window.addEventListener('scroll', positionMenu, true);

        return () => {
            window.removeEventListener('resize', positionMenu);
            window.removeEventListener('scroll', positionMenu, true);
        };
    }, [flatOptions.length, isOpen, maxMenuHeight, optionGroups.length]);

    useEffect(() => {
        if (!isOpen) return undefined;

        function closeOnOutsideClick(event: MouseEvent) {
            const target = event.target as Node;
            if (
                !triggerRef.current?.contains(target)
                && !menuRef.current?.contains(target)
            ) {
                setIsOpen(false);
            }
        }

        document.addEventListener('mousedown', closeOnOutsideClick);
        return () => document.removeEventListener('mousedown', closeOnOutsideClick);
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen || activeIndex < 0) return;
        document.getElementById(`${listboxId}-option-${activeIndex}`)
            ?.scrollIntoView?.({ block: 'nearest' });
    }, [activeIndex, isOpen, listboxId]);

    function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
        if (event.key === 'Escape' && isOpen) {
            event.preventDefault();
            event.stopPropagation();
            setIsOpen(false);
            return;
        }

        if (event.key === 'ArrowDown') {
            event.preventDefault();
            if (!isOpen) {
                openMenu();
            } else {
                setActiveIndex((current) => nextEnabledIndex(current, 1));
            }
            return;
        }

        if (event.key === 'ArrowUp') {
            event.preventDefault();
            if (!isOpen) {
                openMenu(lastEnabledIndex());
            } else {
                setActiveIndex((current) => nextEnabledIndex(current, -1));
            }
            return;
        }

        if (isOpen && event.key === 'Home') {
            event.preventDefault();
            setActiveIndex(firstEnabledIndex());
            return;
        }

        if (isOpen && event.key === 'End') {
            event.preventDefault();
            setActiveIndex(lastEnabledIndex());
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            if (!isOpen) {
                openMenu();
            } else if (activeIndex >= 0) {
                selectOption(flatOptions[activeIndex]);
            }
        }
    }

    const rootClassName = [
        'custom-select',
        `custom-select--${size}`,
        bare ? 'custom-select--bare' : '',
        isOpen ? 'custom-select--open' : '',
        className ?? '',
    ].filter(Boolean).join(' ');
    const menuStyle: CSSProperties | undefined = menuPosition
        ? {
            top: menuPosition.top,
            left: menuPosition.left,
            width: menuPosition.width,
            maxHeight: menuPosition.maxHeight,
        }
        : undefined;

    return (
        <div className={rootClassName}>
            {name && <input type="hidden" name={name} value={value} />}
            <button
                ref={triggerRef}
                type="button"
                className="custom-select__trigger"
                role="combobox"
                aria-label={ariaLabel}
                aria-labelledby={ariaLabelledBy}
                aria-controls={listboxId}
                aria-expanded={isOpen}
                aria-haspopup="listbox"
                aria-activedescendant={isOpen && activeIndex >= 0
                    ? `${listboxId}-option-${activeIndex}`
                    : undefined}
                disabled={disabled}
                onClick={() => (isOpen ? setIsOpen(false) : openMenu())}
                onKeyDown={handleKeyDown}
            >
                {leadingIcon && (
                    <span className="custom-select__leading" aria-hidden="true">
                        {leadingIcon}
                    </span>
                )}
                <span className={`custom-select__value${selectedOption ? '' : ' custom-select__placeholder'}`}>
                    {selectedOption?.label ?? placeholder}
                </span>
                <ChevronDown className="custom-select__chevron" size={16} aria-hidden="true" />
            </button>

            {isOpen && menuPosition && createPortal(
                <div
                    ref={menuRef}
                    id={listboxId}
                    className="custom-select__menu"
                    role="listbox"
                    aria-label={ariaLabel}
                    aria-labelledby={ariaLabelledBy}
                    style={menuStyle}
                >
                    {optionGroups.map((group, groupIndex) => {
                        const groupStartIndex = optionGroups
                            .slice(0, groupIndex)
                            .reduce((total, item) => total + item.options.length, 0);

                        return (
                            <div
                                key={group.label || `options-${groupIndex}`}
                                className="custom-select__group"
                                role={group.label ? 'group' : 'presentation'}
                                aria-label={group.label || undefined}
                            >
                                {group.label && (
                                    <div className="custom-select__group-label">
                                        {group.label}
                                    </div>
                                )}
                                {group.options.map((option, optionIndex) => {
                                    const index = groupStartIndex + optionIndex;
                                    const isSelected = option.value === value;
                                    const isActive = index === activeIndex;

                                    return (
                                        <button
                                            key={option.value}
                                            id={`${listboxId}-option-${index}`}
                                            type="button"
                                            className={[
                                                'custom-select__option',
                                                isActive ? 'custom-select__option--active' : '',
                                                isSelected ? 'custom-select__option--selected' : '',
                                            ].filter(Boolean).join(' ')}
                                            role="option"
                                            aria-selected={isSelected}
                                            disabled={option.disabled}
                                            onMouseEnter={() => {
                                                if (!option.disabled) setActiveIndex(index);
                                            }}
                                            onClick={() => selectOption(option)}
                                        >
                                            <span>{option.label}</span>
                                            {isSelected && <Check size={15} aria-hidden="true" />}
                                        </button>
                                    );
                                })}
                            </div>
                        );
                    })}
                </div>,
                document.body,
            )}
        </div>
    );
}
