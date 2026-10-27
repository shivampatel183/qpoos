import type { ComponentType, ReactNode } from "react";

export type DataTableAlign = "left" | "center" | "right";
export type DataTableDensity = "compact" | "normal" | "comfortable";
export type SortDirection = "asc" | "desc";

export type SortState = {
  key: string;
  direction: SortDirection;
};

export type DataTableFilterOption = {
  label: string;
  value: string;
};

export type DataTableFilterState = Record<string, string>;

export type DataTableFilter<T> = {
  key: keyof T | string;
  label: string;
  allLabel?: string;
  options?: DataTableFilterOption[];
  getValue?: (row: T) => unknown;
};

export type DataTableColumn<T> = {
  key: keyof T | string;
  header: string;
  sortable?: boolean;
  searchable?: boolean;
  width?: string;
  align?: DataTableAlign;
  sticky?: "left" | "right";
  hideOnMobile?: boolean;
  render?: (row: T) => ReactNode;
};

export type DataTableAction<T> = {
  label: string;
  icon?: ComponentType<{ size?: number }>;
  danger?: boolean;
  disabled?: (row: T) => boolean;
  hidden?: (row: T) => boolean;
  onClick: (row: T) => void;
};

export type DataTableBulkAction<T> = {
  label: string;
  icon?: ComponentType<{ size?: number }>;
  danger?: boolean;
  disabled?: (rows: T[]) => boolean;
  onClick: (rows: T[]) => void;
};

export type DataTablePrimaryActionItem = {
  label: string;
  icon?: ComponentType<{ size?: number }>;
  danger?: boolean;
  onClick: () => void;
};

export type DataTablePrimaryAction = {
  label: string;
  icon?: ComponentType<{ size?: number }>;
  onClick: () => void;
  menu?: DataTablePrimaryActionItem[];
};

export type DataTableEmptyAction = {
  label: string;
  icon?: ComponentType<{ size?: number }>;
  onClick: () => void;
};

export type DataTableProps<T> = {
  data: T[];
  columns: DataTableColumn<T>[];
  rowKey: keyof T | ((row: T) => string);

  title?: string;
  subtitle?: string;

  loading?: boolean;
  loadingText?: string;
  error?: string | null;
  onRetry?: () => void;

  searchable?: boolean;
  searchPlaceholder?: string;

  filters?: DataTableFilter<T>[];
  defaultFilters?: DataTableFilterState;
  filterValues?: DataTableFilterState;

  sortable?: boolean;
  defaultSort?: SortState;

  pagination?: boolean;
  pageSize?: number;
  pageSizeOptions?: number[];

  selectable?: boolean;
  selectedRows?: string[];
  onSelectionChange?: (ids: string[]) => void;

  actions?: DataTableAction<T>[];
  /**
   * Less frequently used row actions, shown from the three-dot menu.
   * Use `actions` for controls that should remain directly visible.
   */
  moreActions?: DataTableAction<T>[];
  stickyActions?: boolean;
  bulkActions?: DataTableBulkAction<T>[];
  /** Render arbitrary content in a full-width row directly below the expanded record. */
  expandableRow?: {
    render: (row: T) => ReactNode;
    label?: (row: T, expanded: boolean) => string;
  };

  primaryAction?: DataTablePrimaryAction;
  toolbarActions?: ReactNode;

  emptyTitle?: string;
  emptyMessage?: string;
  emptyAction?: DataTableEmptyAction;

  manualPagination?: boolean;
  manualSorting?: boolean;
  manualSearch?: boolean;
  manualFiltering?: boolean;
  totalRows?: number;
  page?: number;
  onPageChange?: (page: number) => void;
  onSearchChange?: (value: string) => void;
  onFilterChange?: (filters: DataTableFilterState) => void;
  onSortChange?: (sort: SortState) => void;

  onRowClick?: (row: T) => void;
  density?: DataTableDensity;
};
