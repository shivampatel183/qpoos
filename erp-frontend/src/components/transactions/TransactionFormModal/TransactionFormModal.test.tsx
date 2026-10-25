import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MessagePopupProvider } from "@/components/ui/MessagePopup/MessagePopup";
import { accountingService } from "@/services/accountingService";
import { vendorService } from "@/services/vendorService";
import { TransactionForm } from "./TransactionFormModal";

vi.mock("@/services/accountingService", () => ({
  accountingService: { listLedgers: vi.fn() },
}));

vi.mock("@/services/vendorService", () => ({
  vendorService: { list: vi.fn() },
}));

vi.mock("@/services/productService", () => ({
  productService: { list: vi.fn() },
}));

vi.mock("@/services/transactionService", () => ({
  transactionService: {
    create: vi.fn(),
    update: vi.fn(),
  },
}));

const ledgers = [
  {
    id: 1,
    companyId: "company-1",
    accountGroupId: 1,
    accountGroupCode: "CASH",
    accountGroupName: "Cash",
    accountTypeId: 1,
    accountTypeCode: "CASH",
    accountTypeName: "Cash",
    code: "CASH_IN_HAND",
    name: "Cash in Hand",
    openingBalance: 0,
    openingBalanceAsOfDate: "2026-01-01",
    balance: 0,
    ledgerType: "CASH" as const,
    normalBalance: "DEBIT" as const,
    active: true,
    systemDefined: true,
  },
  {
    id: 2,
    companyId: "company-1",
    accountGroupId: 2,
    accountGroupCode: "EXPENSE",
    accountGroupName: "Expenses",
    accountTypeId: 2,
    accountTypeCode: "EXPENSE",
    accountTypeName: "Expense",
    code: "RENT_EXPENSE",
    name: "Rent Expense",
    openingBalance: 0,
    openingBalanceAsOfDate: "2026-01-01",
    balance: 0,
    ledgerType: "GENERAL" as const,
    normalBalance: "DEBIT" as const,
    active: true,
    systemDefined: true,
  },
];

describe("TransactionForm account dropdowns", () => {
  beforeEach(() => {
    vi.mocked(accountingService.listLedgers).mockResolvedValue(ledgers);
    vi.mocked(vendorService.list).mockResolvedValue([]);
  });

  it("shows Chart of Accounts ledgers when postingAllowed is omitted by the API", async () => {
    render(
      <MessagePopupProvider>
        <TransactionForm onCancel={vi.fn()} />
      </MessagePopupProvider>,
    );

    const selectors = await screen.findAllByRole("combobox");
    await waitFor(() => expect(selectors[1]).not.toBeDisabled());

    fireEvent.click(selectors[1]);
    expect(
      await screen.findByRole("option", { name: "CASH_IN_HAND - Cash in Hand" }),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("option", { name: "CASH_IN_HAND - Cash in Hand" }));
    expect(selectors[1]).toHaveTextContent("CASH_IN_HAND - Cash in Hand");

    fireEvent.click(selectors[2]);
    expect(
      await screen.findByRole("option", { name: "RENT_EXPENSE - Rent Expense" }),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("option", { name: "RENT_EXPENSE - Rent Expense" }));
    expect(selectors[2]).toHaveTextContent("RENT_EXPENSE - Rent Expense");
  });
});
