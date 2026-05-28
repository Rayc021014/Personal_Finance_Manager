export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Account {
  id: string;
  name: string;
  type: string;
  currency: string;
  initialBalance: number;
  currentBalance: number;
  note: string | null;
}

export interface Category {
  id: string;
  parentId: string | null;
  name: string;
  type: "INCOME" | "EXPENSE" | "BOTH";
  icon: string | null;
  color: string | null;
  system: boolean;
}

export interface Transaction {
  id: string;
  type: "INCOME" | "EXPENSE" | "TRANSFER";
  accountId: string;
  accountName: string;
  categoryId: string | null;
  categoryName: string | null;
  amount: number;
  currency: string;
  transactionDate: string;
  note: string | null;
  transferPairId: string | null;
}

export interface Summary {
  totalIncome: number;
  totalExpense: number;
  netSavings: number;
  savingsRate: number;
}

export interface CategoryBreakdownItem {
  categoryId: string | null;
  categoryName: string;
  type: "INCOME" | "EXPENSE";
  amount: number;
}

export interface Budget {
  id: string;
  categoryId: string;
  categoryName: string;
  year: number;
  month: number;
  amountLimit: number;
}

export interface BudgetStatus {
  budgetId: string;
  categoryId: string;
  categoryName: string;
  amountLimit: number;
  spentAmount: number;
  usageRate: number;
  status: "SAFE" | "WARNING" | "EXCEEDED";
}

export interface TransactionImportResponse {
  successCount: number;
  failedCount: number;
  errors: string[];
}

export interface TransactionPage {
  content: Transaction[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  message?: string;
  error?: string;
  details?: Record<string, string>;
}
