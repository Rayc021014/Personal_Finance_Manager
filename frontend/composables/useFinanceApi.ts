import type {
  Account,
  ApiError,
  Budget,
  BudgetStatus,
  Category,
  CategoryBreakdownItem,
  Summary,
  TokenResponse,
  Transaction,
  TransactionImportResponse,
  TransactionPage
} from "../types/api";

interface RegisterPayload {
  email: string;
  password: string;
  displayName: string;
  currency: string;
}

interface LoginPayload {
  email: string;
  password: string;
}

interface CreateAccountPayload {
  name: string;
  type: string;
  currency: string;
  initialBalance: number;
  note: string;
}

interface CreateCategoryPayload {
  parentId: null;
  name: string;
  type: string;
  icon: string;
  color: string;
}

interface CreateTransactionPayload {
  type: "INCOME" | "EXPENSE";
  accountId: string;
  categoryId: string;
  amount: number;
  transactionDate: string;
  note: string;
}

interface UpdateTransactionPayload {
  accountId: string;
  categoryId: string;
  amount: number;
  transactionDate: string;
  note: string;
}

interface TransferPayload {
  fromAccountId: string;
  toAccountId: string;
  categoryId: string;
  amount: number;
  transactionDate: string;
  note: string;
}

interface DateRangePayload {
  startDate: string;
  endDate: string;
}

interface BudgetPayload {
  categoryId: string;
  year: number;
  month: number;
  amountLimit: number;
}

interface TransactionQuery {
  accountId?: string;
  categoryId?: string;
  type?: "INCOME" | "EXPENSE" | "TRANSFER";
  startDate?: string;
  endDate?: string;
  keyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}

interface UseFinanceApi {
  accessToken: ReturnType<typeof useState<string>>;
  refreshToken: ReturnType<typeof useState<string>>;
  preferredCurrency: ReturnType<typeof useState<string>>;
  hydrateAuth: () => void;
  ensureSession: () => Promise<boolean>;
  persistAuth: (tokens: TokenResponse, currency?: string) => void;
  clearAuth: () => void;
  register: (payload: RegisterPayload) => Promise<TokenResponse>;
  login: (payload: LoginPayload) => Promise<TokenResponse>;
  getAccounts: () => Promise<Account[]>;
  getCategories: () => Promise<Category[]>;
  getTransactions: (query?: TransactionQuery) => Promise<TransactionPage>;
  getSummary: (range: DateRangePayload) => Promise<Summary>;
  getCategoryBreakdown: (range: DateRangePayload) => Promise<CategoryBreakdownItem[]>;
  getBudgets: (year: number, month: number) => Promise<Budget[]>;
  getBudgetStatus: (year: number, month: number) => Promise<BudgetStatus[]>;
  createBudget: (payload: BudgetPayload) => Promise<Budget>;
  updateBudget: (budgetId: string, payload: BudgetPayload) => Promise<Budget>;
  deleteBudget: (budgetId: string) => Promise<void>;
  copyBudgets: (from: string, to: string) => Promise<Budget[]>;
  createAccount: (payload: CreateAccountPayload) => Promise<Account>;
  createCategory: (payload: CreateCategoryPayload) => Promise<Category>;
  createTransaction: (payload: CreateTransactionPayload) => Promise<Transaction>;
  createTransfer: (payload: TransferPayload) => Promise<Transaction>;
  importTransactionsCsv: (accountId: string, file: File) => Promise<TransactionImportResponse>;
  updateTransaction: (transactionId: string, payload: UpdateTransactionPayload) => Promise<Transaction>;
  deleteTransaction: (transactionId: string) => Promise<void>;
}

function formatApiError(error: ApiError | null | undefined) {
  if (error?.details) {
    const detail = Object.entries(error.details)
      .map(([field, message]) => `${field}: ${message}`)
      .join(" | ");
    if (detail) {
      return detail;
    }
  }

  return error?.message || error?.error || "Request failed";
}

function buildQueryString(query: TransactionQuery = {}) {
  const params = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    params.set(key, String(value));
  });

  return params.toString();
}

export function useFinanceApi(): UseFinanceApi {
  const config = useRuntimeConfig();
  const accessToken = useState<string>("access-token", () => "");
  const refreshToken = useState<string>("refresh-token", () => "");
  const preferredCurrency = useState<string>("preferred-currency", () => "TWD");
  let refreshPromise: Promise<void> | null = null;

  const persistAuth = (tokens: TokenResponse, currency?: string) => {
    accessToken.value = tokens.accessToken;
    refreshToken.value = tokens.refreshToken;
    if (currency) {
      preferredCurrency.value = currency.toUpperCase();
    }

    if (import.meta.client) {
      localStorage.setItem("pfm.accessToken", accessToken.value);
      localStorage.setItem("pfm.refreshToken", refreshToken.value);
      localStorage.setItem("pfm.currency", preferredCurrency.value);
    }
  };

  const clearAuth = () => {
    accessToken.value = "";
    refreshToken.value = "";
    if (import.meta.client) {
      localStorage.removeItem("pfm.accessToken");
      localStorage.removeItem("pfm.refreshToken");
    }
  };

  const hydrateAuth = () => {
    if (!import.meta.client) {
      return;
    }

    accessToken.value = localStorage.getItem("pfm.accessToken") || "";
    refreshToken.value = localStorage.getItem("pfm.refreshToken") || "";
    preferredCurrency.value = localStorage.getItem("pfm.currency") || "TWD";

    if (!accessToken.value || !refreshToken.value) {
      clearAuth();
    }
  };

  const refresh = async () => {
    if (!refreshToken.value) {
      clearAuth();
      throw new Error("Session expired");
    }

    const response = await fetch(`${config.public.apiBase}/api/v1/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ refreshToken: refreshToken.value })
    });

    const payload = (await response.json().catch(() => null)) as TokenResponse | ApiError | null;
    if (!response.ok) {
      clearAuth();
      throw new Error(formatApiError(payload as ApiError));
    }

    persistAuth(payload as TokenResponse);
  };

  const ensureFreshAccessToken = async () => {
    if (!refreshPromise) {
      refreshPromise = refresh().finally(() => {
        refreshPromise = null;
      });
    }
    await refreshPromise;
  };

  const ensureSession = async () => {
    if (!accessToken.value && !refreshToken.value) {
      return false;
    }
    if (!accessToken.value || !refreshToken.value) {
      clearAuth();
      return false;
    }

    try {
      await ensureFreshAccessToken();
      return true;
    } catch {
      return false;
    }
  };

  const rawRequest = async <T>(path: string, options: RequestInit = {}, retry = true): Promise<T> => {
    const headers = new Headers(options.headers || {});
    if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
      headers.set("Content-Type", "application/json");
    }
    if (accessToken.value) {
      headers.set("Authorization", `Bearer ${accessToken.value}`);
    }

    const response = await fetch(`${config.public.apiBase}${path}`, {
      ...options,
      headers
    });

    if ((response.status === 401 || response.status === 403) && retry && refreshToken.value) {
      await ensureFreshAccessToken();
      return rawRequest<T>(path, options, false);
    }

    if (response.status === 204) {
      return null as T;
    }

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(formatApiError(payload));
    }

    return payload as T;
  };

  return {
    accessToken,
    refreshToken,
    preferredCurrency,
    hydrateAuth,
    ensureSession,
    persistAuth,
    clearAuth,
    register: (payload: RegisterPayload) => rawRequest<TokenResponse>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(payload)
    }, false),
    login: (payload: LoginPayload) => rawRequest<TokenResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    }, false),
    getAccounts: () => rawRequest<Account[]>("/api/v1/accounts"),
    getCategories: () => rawRequest<Category[]>("/api/v1/categories"),
    getTransactions: (query: TransactionQuery = {}) => {
      const queryString = buildQueryString({
        page: 0,
        size: 8,
        sort: "transactionDate,desc",
        ...query
      });
      return rawRequest<TransactionPage>(`/api/v1/transactions?${queryString}`);
    },
    getSummary: (range: DateRangePayload) =>
      rawRequest<Summary>(`/api/v1/reports/summary?startDate=${range.startDate}&endDate=${range.endDate}`),
    getCategoryBreakdown: (range: DateRangePayload) =>
      rawRequest<CategoryBreakdownItem[]>(`/api/v1/reports/category-breakdown?startDate=${range.startDate}&endDate=${range.endDate}`),
    getBudgets: (year: number, month: number) =>
      rawRequest<Budget[]>(`/api/v1/budgets?year=${year}&month=${month}`),
    getBudgetStatus: (year: number, month: number) =>
      rawRequest<BudgetStatus[]>(`/api/v1/budgets/status?year=${year}&month=${month}`),
    createBudget: (payload: BudgetPayload) => rawRequest<Budget>("/api/v1/budgets", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
    updateBudget: (budgetId: string, payload: BudgetPayload) =>
      rawRequest<Budget>(`/api/v1/budgets/${budgetId}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      }),
    deleteBudget: (budgetId: string) =>
      rawRequest<void>(`/api/v1/budgets/${budgetId}`, {
        method: "DELETE"
      }),
    copyBudgets: (from: string, to: string) =>
      rawRequest<Budget[]>(`/api/v1/budgets/copy?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`, {
        method: "POST"
      }),
    createAccount: (payload: CreateAccountPayload) => rawRequest<Account>("/api/v1/accounts", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
    createCategory: (payload: CreateCategoryPayload) => rawRequest<Category>("/api/v1/categories", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
    createTransaction: (payload: CreateTransactionPayload) => rawRequest<Transaction>("/api/v1/transactions", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
    createTransfer: (payload: TransferPayload) => rawRequest<Transaction>("/api/v1/transactions/transfer", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
    importTransactionsCsv: (accountId: string, file: File) => {
      const formData = new FormData();
      formData.append("file", file);

      return rawRequest<TransactionImportResponse>(`/api/v1/transactions/import?accountId=${encodeURIComponent(accountId)}`, {
        method: "POST",
        body: formData
      });
    },
    updateTransaction: (transactionId: string, payload: UpdateTransactionPayload) =>
      rawRequest<Transaction>(`/api/v1/transactions/${transactionId}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      }),
    deleteTransaction: (transactionId: string) =>
      rawRequest<void>(`/api/v1/transactions/${transactionId}`, {
        method: "DELETE"
      })
  };
}
