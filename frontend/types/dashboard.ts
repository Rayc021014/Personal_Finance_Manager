export type WorkspaceTab = "overview" | "activity" | "planning" | "setup";

export interface WorkspaceTabItem {
  id: WorkspaceTab;
  eyebrow: string;
  label: string;
  description: string;
  badge: string;
}

export interface CreateTransactionFormState {
  type: "INCOME" | "EXPENSE";
  accountId: string;
  categoryId: string;
  amount: number | undefined;
  transactionDate: string;
  note: string;
}

export interface EditTransactionFormState {
  accountId: string;
  categoryId: string;
  amount: number | undefined;
  transactionDate: string;
  note: string;
}

export interface TransactionFiltersState {
  accountId: string;
  categoryId: string;
  type: "ALL" | "INCOME" | "EXPENSE" | "TRANSFER";
  keyword: string;
  startDate: string;
  endDate: string;
  page: number;
  size: number;
}

export interface BudgetFormState {
  categoryId: string;
  amountLimit: number | undefined;
}

export interface TransferFormState {
  fromAccountId: string;
  toAccountId: string;
  categoryId: string;
  amount: number | undefined;
  transactionDate: string;
  note: string;
}

export interface ImportState {
  accountId: string;
  fileName: string;
  summary: {
    successCount: number;
    failedCount: number;
    errors: string[];
  } | null;
}

export interface BudgetProgressCard {
  budgetId: string;
  categoryId: string;
  categoryName: string;
  amountLimit: number;
  spentAmount: number;
  usageRate: number;
  status: "SAFE" | "WARNING" | "EXCEEDED";
  clampedRate: number;
  remaining: number;
}
