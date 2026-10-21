import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import {
  ArrowDown,
  ArrowUp,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronsUpDown,
  Funnel,
  MoreHorizontal,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { useMessagePopup } from "@/components/ui/MessagePopup";
import { CustomSelect } from "@/components/ui/CustomSelect";
import { Spinner } from "@/components/ui/Spinner";
import type {
  DataTableAction,
  DataTableBulkAction,
  DataTableColumn,
  DataTableFilter,
  DataTableFilterOption,
  DataTableProps,
  DataTableFilterState,
  DataTablePrimaryActionItem,
  SortState,
} from "./DataTable.types";
import "./DataTable.css";

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

function getValue<T>(row: T, key: keyof T | string): unknown {
  return row[key as keyof T];
}

function getSearchText(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (typeof value === "string") return value;
  if (typeof value === "number" || typeof value === "boolean")
    return String(value);
  return "";
}

function compareValues(a: unknown, b: unknown): number {
  if (a === null || a === undefined) return -1;
  if (b === null || b === undefined) return 1;

  if (typeof a === "number" && typeof b === "number") {
    return a - b;
  }

  return String(a).localeCompare(String(b), undefined, {
    numeric: true,
    sensitivity: "base",
  });
}

function getRowId<T>(row: T, rowKey: DataTableProps<T>["rowKey"]): string {
  if (typeof rowKey === "function") return rowKey(row);
  return String(row[rowKey]);
}

function clampPage(page: number, totalPages: number): number {
  if (totalPages <= 1) return 1;
  return Math.min(Math.max(page, 1), totalPages);
}

function getFilterValue<T>(row: T, filter: DataTableFilter<T>): string {
  const value = filter.getValue
    ? filter.getValue(row)
    : getValue(row, filter.key);
  return getSearchText(value);
}

function getFilterOptions<T>(
  data: T[],
  filter: DataTableFilter<T>,
): DataTableFilterOption[] {
  if (filter.options) return filter.options;

  return Array.from(
    new Set(data.map((row) => getFilterValue(row, filter)).filter(Boolean)),
  )
    .sort((left, right) =>
      left.localeCompare(right, undefined, {
        numeric: true,
        sensitivity: "base",
      }),
    )
    .map((value) => ({ label: value, value }));
}

function DataTableActionButton<T>({
  action,
  row,
}: {
  action: DataTableAction<T>;
  row: T;
}) {
  const Icon = action.icon ?? MoreHorizontal;
  const disabled = action.disabled?.(row) ?? false;

  return (
    <button
      type="button"
      className={`data-table__icon-btn${action.danger ? " data-table__icon-btn--danger" : ""}`}
      onClick={(event) => {
        event.stopPropagation();
        action.onClick(row);
      }}
      disabled={disabled}
      title={action.label}
      aria-label={action.label}
    >
      <Icon size={16} />
    </button>
  );
}

function DataTableMoreActions<T>({
  actions,
  row,
}: {
  actions: DataTableAction<T>[];
  row: T;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });

  useEffect(() => {
    if (!isOpen) return undefined;

    const triggerRect = triggerRef.current?.getBoundingClientRect();
    if (triggerRect) {
      const menuWidth = 180;
      const menuHeight = actions.length * 34 + 8;
      const preferredTop = triggerRect.bottom + 6;
      setMenuPosition({
        top:
          preferredTop + menuHeight <= window.innerHeight - 8
            ? preferredTop
            : Math.max(8, triggerRect.top - menuHeight - 6),
        left: Math.max(
          8,
          Math.min(
            triggerRect.right - menuWidth,
            window.innerWidth - menuWidth - 8,
          ),
        ),
      });
    }

    function closeOnOutsideClick(event: MouseEvent) {
      const target = event.target as Node;
      if (
        !triggerRef.current?.contains(target) &&
        !menuRef.current?.contains(target)
      ) {
        setIsOpen(false);
      }
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }

    function closeOnViewportChange() {
      setIsOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    window.addEventListener("resize", closeOnViewportChange);
    window.addEventListener("scroll", closeOnViewportChange, true);

    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
      window.removeEventListener("resize", closeOnViewportChange);
      window.removeEventListener("scroll", closeOnViewportChange, true);
    };
  }, [actions.length, isOpen]);

  return (
    <div
      className="data-table__more-actions"
      onClick={(event) => event.stopPropagation()}
    >
      <button
        ref={triggerRef}
        type="button"
        className="data-table__icon-btn"
        onClick={() => setIsOpen((open) => !open)}
        title="More actions"
        aria-label="More actions"
        aria-haspopup="menu"
        aria-expanded={isOpen}
      >
        <MoreHorizontal size={16} />
      </button>

      {isOpen &&
        createPortal(
          <div
            ref={menuRef}
            className="data-table__action-menu"
            role="menu"
            style={menuPosition}
            onClick={(event) => event.stopPropagation()}
          >
            {actions.map((action) => {
              const Icon = action.icon;
              const disabled = action.disabled?.(row) ?? false;

              return (
                <button
                  key={action.label}
                  type="button"
                  className={`data-table__action-menu-item${action.danger ? " data-table__action-menu-item--danger" : ""}`}
                  role="menuitem"
                  disabled={disabled}
                  onClick={() => {
                    action.onClick(row);
                    setIsOpen(false);
                  }}
                >
                  {Icon && <Icon size={16} />}
                  <span>{action.label}</span>
                </button>
              );
            })}
          </div>,
          document.body,
        )}
    </div>
  );
}

function DataTablePrimaryActionMenu({
  items,
}: {
  items: DataTablePrimaryActionItem[];
}) {
  const [isOpen, setIsOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });

  useEffect(() => {
    if (!isOpen) return undefined;

    const triggerRect = triggerRef.current?.getBoundingClientRect();
    if (triggerRect) {
      const menuWidth = 190;
      const menuHeight = items.length * 34 + 8;
      const preferredTop = triggerRect.bottom + 6;
      setMenuPosition({
        top:
          preferredTop + menuHeight <= window.innerHeight - 8
            ? preferredTop
            : Math.max(8, triggerRect.top - menuHeight - 6),
        left: Math.max(
          8,
          Math.min(
            triggerRect.right - menuWidth,
            window.innerWidth - menuWidth - 8,
          ),
        ),
      });
    }

    function closeOnOutsideClick(event: MouseEvent) {
      const target = event.target as Node;
      if (
        !triggerRef.current?.contains(target) &&
        !menuRef.current?.contains(target)
      ) {
        setIsOpen(false);
      }
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }

    function closeOnViewportChange() {
      setIsOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    window.addEventListener("resize", closeOnViewportChange);
    window.addEventListener("scroll", closeOnViewportChange, true);

    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
      window.removeEventListener("resize", closeOnViewportChange);
      window.removeEventListener("scroll", closeOnViewportChange, true);
    };
  }, [isOpen, items.length]);

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        className="data-table__primary-menu-btn"
        onClick={() => setIsOpen((open) => !open)}
        aria-label="More creation options"
        aria-haspopup="menu"
        aria-expanded={isOpen}
      >
        <ChevronDown size={16} aria-hidden="true" />
      </button>

      {isOpen &&
        createPortal(
          <div
            ref={menuRef}
            className="data-table__action-menu"
            role="menu"
            style={{ ...menuPosition, width: 190 }}
            onClick={(event) => event.stopPropagation()}
          >
            {items.map((item) => {
              const Icon = item.icon;
              return (
                <button
                  key={item.label}
                  type="button"
                  className={`data-table__action-menu-item${item.danger ? " data-table__action-menu-item--danger" : ""}`}
                  role="menuitem"
                  onClick={() => {
                    setIsOpen(false);
                    item.onClick();
                  }}
                >
                  {Icon && <Icon size={15} />}
                  {item.label}
                </button>
              );
            })}
          </div>,
          document.body,
        )}
    </>
  );
}

function DataTableBulkButton<T>({
  action,
  rows,
}: {
  action: DataTableBulkAction<T>;
  rows: T[];
}) {
  const Icon = action.icon;
  const disabled = rows.length === 0 || (action.disabled?.(rows) ?? false);

  return (
    <button
      type="button"
      className={`data-table__bulk-btn${action.danger ? " data-table__bulk-btn--danger" : ""}`}
      onClick={() => action.onClick(rows)}
      disabled={disabled}
    >
      {Icon && <Icon size={15} />}
      {action.label}
    </button>
  );
}

export function DataTable<T>({
  data,
  columns,
  rowKey,
  title,
  subtitle,
  loading = false,
  loadingText = "Loading data...",
  error = null,
  onRetry,
  searchable = false,
  searchPlaceholder = "Search...",
  filters = [],
  defaultFilters = {},
  filterValues,
  sortable = true,
  defaultSort,
  pagination = true,
  pageSize = DEFAULT_PAGE_SIZE,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  selectable = false,
  selectedRows,
  onSelectionChange,
  actions = [],
  moreActions = [],
  stickyActions = true,
  bulkActions = [],
  primaryAction,
  toolbarActions,
  emptyTitle = "No records found",
  emptyMessage = "There is no data to show right now.",
  emptyAction,
  manualPagination = false,
  manualSorting = false,
  manualSearch = false,
  manualFiltering = false,
  totalRows,
  page,
  onPageChange,
  onSearchChange,
  onFilterChange,
  onSortChange,
  onRowClick,
  density = "normal",
}: DataTableProps<T>) {
  const messagePopup = useMessagePopup();
  const lastError = useRef<string | null>(null);
  const [internalSearch, setInternalSearch] = useState("");
  const [internalSort, setInternalSort] = useState<SortState | undefined>(
    defaultSort,
  );
  const [internalPage, setInternalPage] = useState(1);
  const [internalPageSize, setInternalPageSize] = useState(pageSize);
  const [internalSelectedRows, setInternalSelectedRows] = useState<string[]>(
    [],
  );
  const [internalFilters, setInternalFilters] =
    useState<DataTableFilterState>(defaultFilters);

  const searchValue = internalSearch;
  const sortState = internalSort;
  const activeFilters = filterValues ?? internalFilters;
  const currentPage = page ?? internalPage;
  const activePageSize = internalPageSize;
  const activeSelectedRows = selectedRows ?? internalSelectedRows;

  const searchableColumns = useMemo(
    () => columns.filter((column) => column.searchable !== false),
    [columns],
  );

  const filterOptions = useMemo(
    () =>
      new Map(
        filters.map((filter) => [
          String(filter.key),
          getFilterOptions(data, filter),
        ]),
      ),
    [data, filters],
  );

  const processedData = useMemo(() => {
    let nextData = [...data];

    if (!manualFiltering) {
      const appliedFilters = filters.filter((filter) =>
        Boolean(activeFilters[String(filter.key)]),
      );

      if (appliedFilters.length > 0) {
        nextData = nextData.filter((row) =>
          appliedFilters.every(
            (filter) =>
              getFilterValue(row, filter) === activeFilters[String(filter.key)],
          ),
        );
      }
    }

    if (searchable && !manualSearch && searchValue.trim()) {
      const term = searchValue.trim().toLowerCase();
      nextData = nextData.filter((row) =>
        searchableColumns.some((column) =>
          getSearchText(getValue(row, column.key)).toLowerCase().includes(term),
        ),
      );
    }

    if (sortState && !manualSorting) {
      const directionMultiplier = sortState.direction === "asc" ? 1 : -1;
      nextData.sort(
        (left, right) =>
          compareValues(
            getValue(left, sortState.key),
            getValue(right, sortState.key),
          ) * directionMultiplier,
      );
    }

    return nextData;
  }, [
    activeFilters,
    data,
    filters,
    manualFiltering,
    manualSearch,
    manualSorting,
    searchValue,
    searchable,
    searchableColumns,
    sortState,
  ]);

  const rowCount = manualPagination
    ? (totalRows ?? data.length)
    : processedData.length;
  const totalPages = pagination
    ? Math.max(Math.ceil(rowCount / activePageSize), 1)
    : 1;
  const safePage = clampPage(currentPage, totalPages);

  const visibleData = useMemo(() => {
    if (!pagination || manualPagination) return processedData;
    const start = (safePage - 1) * activePageSize;
    return processedData.slice(start, start + activePageSize);
  }, [activePageSize, manualPagination, pagination, processedData, safePage]);

  const visibleRowIds = useMemo(
    () => visibleData.map((row) => getRowId(row, rowKey)),
    [rowKey, visibleData],
  );

  const selectedData = useMemo(
    () =>
      data.filter((row) => activeSelectedRows.includes(getRowId(row, rowKey))),
    [activeSelectedRows, data, rowKey],
  );

  const allVisibleSelected =
    visibleRowIds.length > 0 &&
    visibleRowIds.every((id) => activeSelectedRows.includes(id));

  const hasActions = actions.length > 0 || moreActions.length > 0;
  const hasHeader =
    title ||
    subtitle ||
    primaryAction ||
    toolbarActions ||
    searchable ||
    filters.length > 0;
  const hasActiveFilters = Object.values(activeFilters).some(Boolean);
  const firstItem = rowCount === 0 ? 0 : (safePage - 1) * activePageSize + 1;
  const lastItem = pagination
    ? Math.min(safePage * activePageSize, rowCount)
    : rowCount;

  useEffect(() => {
    if (error && error !== lastError.current) {
      lastError.current = error;
      messagePopup.error(error);
    }

    if (!error) {
      lastError.current = null;
    }
  }, [error, messagePopup]);

  function updateSelection(ids: string[]) {
    if (selectedRows) {
      onSelectionChange?.(ids);
      return;
    }

    setInternalSelectedRows(ids);
    onSelectionChange?.(ids);
  }

  function updatePage(nextPage: number) {
    const clampedPage = clampPage(nextPage, totalPages);
    if (page === undefined) setInternalPage(clampedPage);
    onPageChange?.(clampedPage);
  }

  function updateSearch(value: string) {
    setInternalSearch(value);
    if (page === undefined) setInternalPage(1);
    onSearchChange?.(value);
  }

  function updateFilter(key: string, value: string) {
    const nextFilters = { ...activeFilters };
    if (value) {
      nextFilters[key] = value;
    } else {
      delete nextFilters[key];
    }

    if (filterValues === undefined) setInternalFilters(nextFilters);
    if (page === undefined) setInternalPage(1);
    onFilterChange?.(nextFilters);
  }

  function clearFilters() {
    if (filterValues === undefined) setInternalFilters({});
    if (page === undefined) setInternalPage(1);
    onFilterChange?.({});
  }

  function updateSort(column: DataTableColumn<T>) {
    if (!sortable || column.sortable === false) return;

    const columnKey = String(column.key);
    const nextSort: SortState = {
      key: columnKey,
      direction:
        sortState?.key === columnKey && sortState.direction === "asc"
          ? "desc"
          : "asc",
    };

    setInternalSort(nextSort);
    onSortChange?.(nextSort);
  }

  function toggleRow(rowId: string) {
    const nextSelected = activeSelectedRows.includes(rowId)
      ? activeSelectedRows.filter((id) => id !== rowId)
      : [...activeSelectedRows, rowId];

    updateSelection(nextSelected);
  }

  function toggleVisibleRows() {
    if (allVisibleSelected) {
      updateSelection(
        activeSelectedRows.filter((id) => !visibleRowIds.includes(id)),
      );
      return;
    }

    updateSelection(
      Array.from(new Set([...activeSelectedRows, ...visibleRowIds])),
    );
  }

  function renderSortIcon(column: DataTableColumn<T>) {
    if (!sortable || column.sortable === false) return null;

    const columnKey = String(column.key);
    if (sortState?.key !== columnKey) {
      return <ChevronsUpDown size={14} className="data-table__sort-icon" />;
    }

    return sortState.direction === "asc" ? (
      <ArrowUp
        size={14}
        className="data-table__sort-icon data-table__sort-icon--active"
      />
    ) : (
      <ArrowDown
        size={14}
        className="data-table__sort-icon data-table__sort-icon--active"
      />
    );
  }

  return (
    <section className={`data-table data-table--${density}`}>
      {hasHeader && (
        <div className="data-table__header">
          {(title || subtitle) && (
            <div className="data-table__heading">
              {title && <h2 className="data-table__title">{title}</h2>}
              <div className="data-table__heading-actions">
                {searchable && (
                  <label className="data-table__search">
                    <Search size={16} />
                    <input
                      value={searchValue}
                      onChange={(event) => updateSearch(event.target.value)}
                      placeholder={searchPlaceholder}
                    />
                  </label>
                )}
                {primaryAction && (
                  <div
                    className={`data-table__primary-action${primaryAction.menu ? " data-table__primary-action--menu" : ""}`}
                  >
                    <button
                      type="button"
                      className="data-table__primary-btn"
                      onClick={primaryAction.onClick}
                    >
                      {primaryAction.icon && <primaryAction.icon size={16} />}
                      {primaryAction.label}
                    </button>
                    {primaryAction.menu && (
                      <DataTablePrimaryActionMenu items={primaryAction.menu} />
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          <div className="data-table__toolbar">
            {filters.map((filter) => {
              const filterKey = String(filter.key);
              return (
                <CustomSelect
                  key={filterKey}
                  className="data-table__filter"
                  value={activeFilters[filterKey] ?? ""}
                  onChange={(nextValue) => updateFilter(filterKey, nextValue)}
                  options={[
                    {
                      value: "",
                      label: filter.allLabel ?? `All ${filter.label}`,
                    },
                    ...(filterOptions.get(filterKey) ?? []),
                  ]}
                  ariaLabel={filter.label}
                  leadingIcon={<Funnel size={15} />}
                  maxMenuHeight={220}
                  bare
                />
              );
            })}

            {hasActiveFilters && (
              <button
                type="button"
                className="data-table__clear-filters"
                onClick={clearFilters}
                title="Clear filters"
                aria-label="Clear filters"
              >
                <X size={16} />
              </button>
            )}

            {toolbarActions}
          </div>
        </div>
      )}

      {error && (
        <div className="data-table__message">
          {onRetry && (
            <button
              type="button"
              className="data-table__retry-btn"
              onClick={onRetry}
            >
              <RefreshCw size={15} />
              Retry
            </button>
          )}
        </div>
      )}

      {selectable &&
        bulkActions.length > 0 &&
        activeSelectedRows.length > 0 && (
          <div className="data-table__bulk-bar">
            <span>{activeSelectedRows.length} selected</span>
            <div className="data-table__bulk-actions">
              {bulkActions.map((action) => (
                <DataTableBulkButton
                  key={action.label}
                  action={action}
                  rows={selectedData}
                />
              ))}
            </div>
          </div>
        )}

      <div className="data-table__surface">
        <div className="data-table__scroll">
          <table>
            <thead>
              <tr>
                {selectable && (
                  <th className="data-table__select-col">
                    <input
                      type="checkbox"
                      checked={allVisibleSelected}
                      onChange={toggleVisibleRows}
                      aria-label="Select visible rows"
                    />
                  </th>
                )}

                {columns.map((column) => (
                  <th
                    key={String(column.key)}
                    style={{ width: column.width }}
                    className={[
                      `data-table__cell--${column.align ?? "left"}`,
                      column.hideOnMobile
                        ? "data-table__cell--mobile-hidden"
                        : "",
                      sortable && column.sortable !== false
                        ? "data-table__cell--sortable"
                        : "",
                      column.sticky
                        ? `data-table__cell--sticky-${column.sticky}`
                        : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    <button
                      type="button"
                      onClick={() => updateSort(column)}
                      disabled={!sortable || column.sortable === false}
                    >
                      <span>{column.header}</span>
                      {renderSortIcon(column)}
                    </button>
                  </th>
                ))}

                {hasActions && (
                  <th
                    className={[
                      "data-table__actions-col",
                      stickyActions ? "data-table__cell--sticky-right" : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    Actions
                  </th>
                )}
              </tr>
            </thead>

            <tbody>
              {loading && (
                <tr>
                  <td
                    className="data-table__state-cell"
                    colSpan={
                      columns.length + Number(selectable) + Number(hasActions)
                    }
                  >
                    <Spinner size={22} />
                    <span>{loadingText}</span>
                  </td>
                </tr>
              )}

              {!loading && !error && visibleData.length === 0 && (
                <tr>
                  <td
                    className="data-table__empty-cell"
                    colSpan={
                      columns.length + Number(selectable) + Number(hasActions)
                    }
                  >
                    <strong>{emptyTitle}</strong>
                    <span>{emptyMessage}</span>
                    {emptyAction && (
                      <button
                        type="button"
                        className="data-table__primary-btn"
                        onClick={emptyAction.onClick}
                      >
                        {emptyAction.icon && <emptyAction.icon size={16} />}
                        {emptyAction.label}
                      </button>
                    )}
                  </td>
                </tr>
              )}

              {!loading &&
                !error &&
                visibleData.map((row) => {
                  const id = getRowId(row, rowKey);
                  const rowActions = actions.filter(
                    (action) => !(action.hidden?.(row) ?? false),
                  );
                  const rowMoreActions = moreActions.filter(
                    (action) => !(action.hidden?.(row) ?? false),
                  );

                  return (
                    <tr
                      key={id}
                      className={
                        onRowClick ? "data-table__row--clickable" : undefined
                      }
                      onClick={() => onRowClick?.(row)}
                    >
                      {selectable && (
                        <td className="data-table__select-col">
                          <input
                            type="checkbox"
                            checked={activeSelectedRows.includes(id)}
                            onChange={() => toggleRow(id)}
                            onClick={(event) => event.stopPropagation()}
                            aria-label="Select row"
                          />
                        </td>
                      )}

                      {columns.map((column) => (
                        <td
                          key={String(column.key)}
                          className={[
                            `data-table__cell--${column.align ?? "left"}`,
                            column.hideOnMobile
                              ? "data-table__cell--mobile-hidden"
                              : "",
                            column.sticky
                              ? `data-table__cell--sticky-${column.sticky}`
                              : "",
                          ]
                            .filter(Boolean)
                            .join(" ")}
                        >
                          {column.render
                            ? column.render(row)
                            : getSearchText(getValue(row, column.key))}
                        </td>
                      ))}

                      {hasActions && (
                        <td
                          className={[
                            "data-table__actions-col",
                            stickyActions
                              ? "data-table__cell--sticky-right"
                              : "",
                          ]
                            .filter(Boolean)
                            .join(" ")}
                        >
                          <div className="data-table__row-actions">
                            {rowActions.map((action) => (
                              <DataTableActionButton
                                key={action.label}
                                action={action}
                                row={row}
                              />
                            ))}
                            {rowMoreActions.length > 0 && (
                              <DataTableMoreActions
                                actions={rowMoreActions}
                                row={row}
                              />
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
            </tbody>
          </table>
        </div>
      </div>

      {pagination && !loading && !error && rowCount > 0 && (
        <div className="data-table__pagination">
          <div className="data-table__page-summary">
            Showing {firstItem}-{lastItem} of {rowCount}
          </div>

          <div className="data-table__page-controls">
            <CustomSelect
              className="data-table__page-size"
              value={String(activePageSize)}
              onChange={(nextValue) => {
                setInternalPageSize(Number(nextValue));
                updatePage(1);
              }}
              options={pageSizeOptions.map((option) => ({
                value: String(option),
                label: `${option} / page`,
              }))}
              ariaLabel="Rows per page"
              maxMenuHeight={180}
              size="compact"
            />

            <button
              type="button"
              className="data-table__page-btn"
              onClick={() => updatePage(safePage - 1)}
              disabled={safePage <= 1}
              aria-label="Previous page"
            >
              <ChevronLeft size={16} />
            </button>

            <span className="data-table__page-number">
              {safePage} / {totalPages}
            </span>

            <button
              type="button"
              className="data-table__page-btn"
              onClick={() => updatePage(safePage + 1)}
              disabled={safePage >= totalPages}
              aria-label="Next page"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
