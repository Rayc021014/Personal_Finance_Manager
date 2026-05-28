<script setup lang="ts">
import type { Account, Budget, BudgetStatus, Category, CategoryBreakdownItem, Summary, Transaction, TransactionImportResponse, TransactionPage } from "../types/api";
import type {
  BudgetFormState,
  BudgetProgressCard,
  CreateTransactionFormState,
  EditTransactionFormState,
  ImportState,
  TransactionFiltersState,
  TransferFormState,
  WorkspaceTab,
  WorkspaceTabItem
} from "../types/dashboard";
import { useFinanceApi } from "../composables/useFinanceApi";
import { computed, onMounted, reactive, ref, watch } from "vue";

const api = useFinanceApi();

type AuthMode = "login" | "register";
type ChartPreset = "month" | "week" | "day" | "custom";
type TransactionKind = "INCOME" | "EXPENSE";
type TransactionFilterType = "ALL" | "INCOME" | "EXPENSE" | "TRANSFER";

const authMode = ref<AuthMode>("login");
const feedback = ref("");
const feedbackTone = ref<"" | "success" | "error">("");
const isAuthenticated = computed(() => Boolean(api.accessToken.value && api.refreshToken.value));
const loading = ref(false);
const transactionLoading = ref(false);
const budgetLoading = ref(false);
const activeWorkspaceTab = ref<WorkspaceTab>("overview");
const editingTransactionId = ref<string | null>(null);
const editingBudgetId = ref<string | null>(null);

const authForm = reactive({
  email: "",
  password: "",
  displayName: "",
  currency: "TWD"
});

const accountForm = reactive({
  name: "",
  type: "BANK",
  currency: "TWD",
  initialBalance: 0,
  note: ""
});

const categoryForm = reactive({
  name: "",
  type: "EXPENSE",
  icon: "",
  color: "#E59B52"
});

const createTransactionForm = reactive<CreateTransactionFormState>({
  type: "EXPENSE" as TransactionKind,
  accountId: "",
  categoryId: "",
  amount: undefined as number | undefined,
  transactionDate: new Date().toISOString().slice(0, 10),
  note: ""
});

const editTransactionForm = reactive<EditTransactionFormState>({
  accountId: "",
  categoryId: "",
  amount: undefined as number | undefined,
  transactionDate: "",
  note: ""
});

const transactionFilters = reactive<TransactionFiltersState>({
  accountId: "",
  categoryId: "",
  type: "ALL" as TransactionFilterType,
  keyword: "",
  startDate: "",
  endDate: "",
  page: 0,
  size: 8
});

const budgetMonth = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
});

const budgetForm = reactive<BudgetFormState>({
  categoryId: "",
  amountLimit: undefined as number | undefined
});

const transferForm = reactive<TransferFormState>({
  fromAccountId: "",
  toAccountId: "",
  categoryId: "",
  amount: undefined as number | undefined,
  transactionDate: new Date().toISOString().slice(0, 10),
  note: ""
});

const importState = reactive<ImportState>({
  accountId: "",
  fileName: "",
  summary: null as TransactionImportResponse | null
});

const accounts = ref<Account[]>([]);
const categories = ref<Category[]>([]);
const transactionPage = ref<TransactionPage | null>(null);
const budgets = ref<Budget[]>([]);
const budgetStatuses = ref<BudgetStatus[]>([]);
const summary = ref<Summary | null>(null);
const breakdown = ref<CategoryBreakdownItem[]>([]);
const chartPalette = ["#C96D3A", "#295C4B", "#D6A24B", "#5B6C8F", "#A14F6B", "#6E8B74"];
const chartPreset = ref<ChartPreset>("month");
const chartRange = reactive({
  startDate: "",
  endDate: ""
});

const transactions = computed(() => transactionPage.value?.content || []);
const heroAccounts = computed(() => accounts.value.length);
const heroTransactions = computed(() => transactionPage.value?.totalElements || 0);
const summaryRate = computed(() => `${((summary.value?.savingsRate || 0) * 100).toFixed(1)}%`);
const netAmount = computed(() => formatCurrency(summary.value?.netSavings || 0, api.preferredCurrency.value));
const workspaceTabs = computed<WorkspaceTabItem[]>(() => [
  {
    id: "overview" as const,
    eyebrow: "Snapshot",
    label: "Overview",
    description: "Summary, balances, and category totals",
    badge: `${accounts.value.length} accounts`
  },
  {
    id: "activity" as const,
    eyebrow: "Capture",
    label: "Transactions",
    description: "Add, transfer, search, and edit ledger entries",
    badge: `${transactionPage.value?.totalElements || 0} entries`
  },
  {
    id: "planning" as const,
    eyebrow: "Guardrails",
    label: "Planning",
    description: "Monthly budgets and spending progress",
    badge: `${budgetStatuses.value.length} tracked`
  },
  {
    id: "setup" as const,
    eyebrow: "Structure",
    label: "Setup",
    description: "Accounts, categories, and CSV imports",
    badge: `${categories.value.length} categories`
  }
]);
const activeWorkspaceMeta = computed(() =>
  workspaceTabs.value.find((tab) => tab.id === activeWorkspaceTab.value) || workspaceTabs.value[0]
);
const transactionResultLabel = computed(() => {
  if (!transactionPage.value) {
    return "No transactions loaded yet.";
  }

  if (!transactionPage.value.totalElements) {
    return "No matching transactions.";
  }

  const start = transactionPage.value.number * transactionPage.value.size + 1;
  const end = start + transactionPage.value.content.length - 1;
  return `Showing ${start}-${end} of ${transactionPage.value.totalElements} transactions`;
});

const activeTransaction = computed(() =>
  transactions.value.find((transaction) => transaction.id === editingTransactionId.value) || null
);

const createFormCategories = computed(() =>
  categories.value.filter((category) => category.type === "BOTH" || category.type === createTransactionForm.type)
);

const editFormCategories = computed(() => {
  const current = activeTransaction.value;
  if (!current || current.type === "TRANSFER") {
    return [];
  }

  return categories.value.filter((category) => category.type === "BOTH" || category.type === current.type);
});

const budgetCategories = computed(() =>
  categories.value.filter((category) => category.type === "EXPENSE" || category.type === "BOTH")
);

const budgetProgressCards = computed<BudgetProgressCard[]>(() =>
  budgetStatuses.value.map((status) => ({
    ...status,
    clampedRate: Math.min(Number(status.usageRate || 0), 1),
    remaining: Number(status.amountLimit || 0) - Number(status.spentAmount || 0)
  }))
);

const budgetMonthLabel = computed(() =>
  new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "long"
  }).format(new Date(budgetMonth.year, budgetMonth.month - 1, 1))
);

const budgetFormTitle = computed(() => editingBudgetId.value ? "Edit budget" : "Create budget");
const transferCategories = computed(() => budgetCategories.value);

const copyBudgetSource = computed(() => {
  const source = new Date(budgetMonth.year, budgetMonth.month - 2, 1);
  const year = source.getFullYear();
  const month = `${source.getMonth() + 1}`.padStart(2, "0");
  return `${year}-${month}`;
});

const categoryColors = computed(() =>
  new Map(categories.value.map((category) => [category.id, category.color || ""]))
);

const breakdownMax = computed(() => Math.max(...breakdown.value.map((row) => row.amount), 1));

const breakdownByType = computed(() => {
  const build = (type: "INCOME" | "EXPENSE") => {
    const items = breakdown.value
      .filter((row) => row.type === type && row.amount > 0)
      .map((row, index) => ({
        ...row,
        key: `${type}-${row.categoryId || row.categoryName}-${index}`,
        color: categoryColors.value.get(row.categoryId || "") || chartPalette[index % chartPalette.length]
      }));

    const total = items.reduce((sum, item) => sum + Number(item.amount || 0), 0);
    let offset = 0;
    const slices = items.map((item) => {
      const share = total > 0 ? item.amount / total : 0;
      const start = offset;
      const end = offset + share * 100;
      offset = end;
      return {
        ...item,
        share,
        segment: `${item.color} ${start.toFixed(2)}% ${end.toFixed(2)}%`
      };
    });

    return {
      total,
      slices,
      gradient: slices.length ? `conic-gradient(${slices.map((slice) => slice.segment).join(", ")})` : ""
    };
  };

  return {
    INCOME: build("INCOME"),
    EXPENSE: build("EXPENSE")
  };
});

watch(createFormCategories, (value) => {
  if (!value.find((item) => item.id === createTransactionForm.categoryId)) {
    createTransactionForm.categoryId = value[0]?.id || "";
  }
}, { immediate: true });

watch(accounts, (value) => {
  if (!value.find((item) => item.id === createTransactionForm.accountId)) {
    createTransactionForm.accountId = value[0]?.id || "";
  }
}, { immediate: true });

watch(accounts, (value) => {
  if (!value.find((item) => item.id === transferForm.fromAccountId)) {
    transferForm.fromAccountId = value[0]?.id || "";
  }

  if (!value.find((item) => item.id === transferForm.toAccountId)) {
    transferForm.toAccountId = value[1]?.id || value[0]?.id || "";
  }
}, { immediate: true });

watch(budgetCategories, (value) => {
  if (editingBudgetId.value) {
    return;
  }

  if (!value.find((item) => item.id === budgetForm.categoryId)) {
    budgetForm.categoryId = value[0]?.id || "";
  }
}, { immediate: true });

watch(transferCategories, (value) => {
  if (!value.find((item) => item.id === transferForm.categoryId)) {
    transferForm.categoryId = value[0]?.id || "";
  }
}, { immediate: true });

watch(accounts, (value) => {
  if (!value.find((item) => item.id === importState.accountId)) {
    importState.accountId = value[0]?.id || "";
  }
}, { immediate: true });

watch(editFormCategories, (value) => {
  if (!editingTransactionId.value) {
    return;
  }

  if (!value.find((item) => item.id === editTransactionForm.categoryId)) {
    editTransactionForm.categoryId = value[0]?.id || "";
  }
});

const setFeedback = (message: string, tone: "" | "success" | "error" = "") => {
  feedback.value = message;
  feedbackTone.value = tone;
};

const formatCurrency = (value: number, currency: string) =>
  new Intl.NumberFormat("zh-TW", {
    style: "currency",
    currency: currency || "TWD",
    maximumFractionDigits: 2
  }).format(Number(value || 0));

const formatShare = (value: number) => `${(value * 100).toFixed(value >= 0.1 ? 0 : 1)}%`;
const formatMonthValue = (year: number, month: number) => `${year}-${`${month}`.padStart(2, "0")}`;

const toDateInputValue = (value: Date) => {
  const offset = value.getTimezoneOffset();
  return new Date(value.getTime() - offset * 60 * 1000).toISOString().slice(0, 10);
};

const getPresetRange = (preset: "month" | "week" | "day") => {
  const now = new Date();
  const current = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  if (preset === "day") {
    const date = toDateInputValue(current);
    return { startDate: date, endDate: date };
  }

  if (preset === "week") {
    const day = current.getDay();
    const diffToMonday = (day + 6) % 7;
    const start = new Date(current);
    start.setDate(current.getDate() - diffToMonday);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    return { startDate: toDateInputValue(start), endDate: toDateInputValue(end) };
  }

  const start = new Date(current.getFullYear(), current.getMonth(), 1);
  const end = new Date(current.getFullYear(), current.getMonth() + 1, 0);
  return { startDate: toDateInputValue(start), endDate: toDateInputValue(end) };
};

const amountClass = (type: string) => type === "EXPENSE" ? "amount-negative" : "amount-positive";
const badgeClass = (type: string) => `transaction-badge is-${type.toLowerCase()}`;
const budgetStatusClass = (status: string) => `budget-pill is-${status.toLowerCase()}`;
const transactionTypeLabel = (type: string) => ({
  INCOME: "Income",
  EXPENSE: "Expense",
  TRANSFER: "Transfer"
}[type] || type);

const normalizeTransactionDateFilters = () => {
  if (transactionFilters.startDate && !transactionFilters.endDate) {
    transactionFilters.endDate = transactionFilters.startDate;
  } else if (!transactionFilters.startDate && transactionFilters.endDate) {
    transactionFilters.startDate = transactionFilters.endDate;
  }
};

const loadChartData = async () => {
  const [summaryResponse, breakdownResponse] = await Promise.all([
    api.getSummary({
      startDate: chartRange.startDate,
      endDate: chartRange.endDate
    }),
    api.getCategoryBreakdown({
      startDate: chartRange.startDate,
      endDate: chartRange.endDate
    })
  ]);

  summary.value = summaryResponse;
  breakdown.value = breakdownResponse;
};

const loadTransactions = async (page = transactionFilters.page) => {
  transactionLoading.value = true;
  try {
    transactionPage.value = await api.getTransactions({
      accountId: transactionFilters.accountId || undefined,
      categoryId: transactionFilters.categoryId || undefined,
      type: transactionFilters.type === "ALL" ? undefined : transactionFilters.type,
      keyword: transactionFilters.keyword.trim() || undefined,
      startDate: transactionFilters.startDate || undefined,
      endDate: transactionFilters.endDate || undefined,
      page,
      size: transactionFilters.size,
      sort: "transactionDate,desc"
    });
    transactionFilters.page = transactionPage.value.number;
  } finally {
    transactionLoading.value = false;
  }
};

const loadBudgets = async () => {
  budgetLoading.value = true;
  try {
    const [budgetResponse, budgetStatusResponse] = await Promise.all([
      api.getBudgets(budgetMonth.year, budgetMonth.month),
      api.getBudgetStatus(budgetMonth.year, budgetMonth.month)
    ]);
    budgets.value = budgetResponse;
    budgetStatuses.value = budgetStatusResponse;
  } finally {
    budgetLoading.value = false;
  }
};

const loadDashboard = async () => {
  const [accountsResponse, categoriesResponse] = await Promise.all([
    api.getAccounts(),
    api.getCategories()
  ]);

  accounts.value = accountsResponse;
  categories.value = categoriesResponse;
  accountForm.currency = api.preferredCurrency.value;

  await Promise.all([
    loadTransactions(transactionFilters.page),
    loadChartData(),
    loadBudgets()
  ]);
};

const reloadAfterTransactionChange = async () => {
  await loadDashboard();
};

const resetCreateTransactionForm = () => {
  createTransactionForm.type = "EXPENSE";
  createTransactionForm.amount = undefined;
  createTransactionForm.transactionDate = new Date().toISOString().slice(0, 10);
  createTransactionForm.note = "";
  createTransactionForm.accountId = accounts.value[0]?.id || "";
  createTransactionForm.categoryId = createFormCategories.value[0]?.id || "";
};

const resetBudgetForm = () => {
  editingBudgetId.value = null;
  budgetForm.categoryId = budgetCategories.value[0]?.id || "";
  budgetForm.amountLimit = undefined;
};

const resetTransferForm = () => {
  transferForm.fromAccountId = accounts.value[0]?.id || "";
  transferForm.toAccountId = accounts.value[1]?.id || accounts.value[0]?.id || "";
  transferForm.categoryId = transferCategories.value[0]?.id || "";
  transferForm.amount = undefined;
  transferForm.transactionDate = new Date().toISOString().slice(0, 10);
  transferForm.note = "";
};

const handleAuthSubmit = async () => {
  loading.value = true;
  setFeedback("Connecting to your workspace...");

  try {
    if (authMode.value === "register") {
      const payload = {
        email: authForm.email.trim(),
        password: authForm.password,
        displayName: authForm.displayName.trim(),
        currency: authForm.currency.trim().toUpperCase()
      };
      const tokens = await api.register(payload);
      api.persistAuth(tokens, payload.currency);
    } else {
      const tokens = await api.login({
        email: authForm.email.trim(),
        password: authForm.password
      });
      api.persistAuth(tokens);
    }

    const initialRange = getPresetRange("month");
    chartRange.startDate = initialRange.startDate;
    chartRange.endDate = initialRange.endDate;
    activeWorkspaceTab.value = "overview";
    await loadDashboard();
    resetCreateTransactionForm();
    authForm.password = "";
    setFeedback(authMode.value === "register" ? "Account created and ready." : "Signed in successfully.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const handleCreateAccount = async () => {
  loading.value = true;
  try {
    await api.createAccount({
      ...accountForm,
      currency: accountForm.currency.trim().toUpperCase()
    });
    await loadDashboard();
    accountForm.name = "";
    accountForm.type = "BANK";
    accountForm.currency = api.preferredCurrency.value;
    accountForm.initialBalance = 0;
    accountForm.note = "";
    setFeedback("Account created.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const handleCreateCategory = async () => {
  loading.value = true;
  try {
    await api.createCategory({
      parentId: null,
      name: categoryForm.name.trim(),
      type: categoryForm.type,
      icon: categoryForm.icon.trim(),
      color: categoryForm.color.trim()
    });
    await loadDashboard();
    categoryForm.name = "";
    categoryForm.type = "EXPENSE";
    categoryForm.icon = "";
    categoryForm.color = "#E59B52";
    setFeedback("Category created.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const handleCreateTransaction = async () => {
  loading.value = true;
  try {
    await api.createTransaction({
      type: createTransactionForm.type,
      accountId: createTransactionForm.accountId,
      categoryId: createTransactionForm.categoryId,
      amount: Number(createTransactionForm.amount),
      transactionDate: createTransactionForm.transactionDate,
      note: createTransactionForm.note.trim()
    });
    transactionFilters.page = 0;
    await reloadAfterTransactionChange();
    resetCreateTransactionForm();
    setFeedback("Transaction captured.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const saveBudget = async () => {
  loading.value = true;
  try {
    const payload = {
      categoryId: budgetForm.categoryId,
      year: budgetMonth.year,
      month: budgetMonth.month,
      amountLimit: Number(budgetForm.amountLimit)
    };

    if (editingBudgetId.value) {
      await api.updateBudget(editingBudgetId.value, payload);
      setFeedback("Budget updated.", "success");
    } else {
      await api.createBudget(payload);
      setFeedback("Budget created.", "success");
    }

    await loadBudgets();
    resetBudgetForm();
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const createTransfer = async () => {
  if (transferForm.fromAccountId === transferForm.toAccountId) {
    setFeedback("Transfer source and destination accounts must be different.", "error");
    return;
  }

  loading.value = true;
  try {
    await api.createTransfer({
      fromAccountId: transferForm.fromAccountId,
      toAccountId: transferForm.toAccountId,
      categoryId: transferForm.categoryId,
      amount: Number(transferForm.amount),
      transactionDate: transferForm.transactionDate,
      note: transferForm.note.trim()
    });
    transactionFilters.page = 0;
    await reloadAfterTransactionChange();
    resetTransferForm();
    setFeedback("Transfer recorded.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const importTransactionsCsv = async (event: Event) => {
  const input = event.target as HTMLInputElement | null;
  const file = input?.files?.[0];

  if (!file) {
    return;
  }

  if (!importState.accountId) {
    setFeedback("Choose an account before importing CSV.", "error");
    if (input) {
      input.value = "";
    }
    return;
  }

  loading.value = true;
  try {
    importState.fileName = file.name;
    importState.summary = await api.importTransactionsCsv(importState.accountId, file);
    transactionFilters.page = 0;
    await reloadAfterTransactionChange();
    setFeedback(`CSV import finished: ${importState.summary.successCount} success, ${importState.summary.failedCount} failed.`, "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
    if (input) {
      input.value = "";
    }
  }
};

const editBudget = (budget: Budget) => {
  activeWorkspaceTab.value = "planning";
  editingBudgetId.value = budget.id;
  budgetForm.categoryId = budget.categoryId;
  budgetForm.amountLimit = Number(budget.amountLimit);
};

const removeBudget = async (budget: Budget) => {
  const confirmed = import.meta.client
    ? window.confirm(`Delete budget for ${budget.categoryName} in ${budgetMonthLabel.value}?`)
    : true;

  if (!confirmed) {
    return;
  }

  loading.value = true;
  try {
    await api.deleteBudget(budget.id);
    await loadBudgets();
    if (editingBudgetId.value === budget.id) {
      resetBudgetForm();
    }
    setFeedback("Budget deleted.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const shiftBudgetMonth = async (offset: number) => {
  const next = new Date(budgetMonth.year, budgetMonth.month - 1 + offset, 1);
  budgetMonth.year = next.getFullYear();
  budgetMonth.month = next.getMonth() + 1;
  resetBudgetForm();
  await loadBudgets();
};

const copyPreviousBudgetMonth = async () => {
  loading.value = true;
  try {
    const created = await api.copyBudgets(copyBudgetSource.value, formatMonthValue(budgetMonth.year, budgetMonth.month));
    await loadBudgets();
    setFeedback(created.length ? `Copied ${created.length} budgets from ${copyBudgetSource.value}.` : `No budgets copied from ${copyBudgetSource.value}.`, "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const applyQuickRange = async (preset: "month" | "week" | "day") => {
  chartPreset.value = preset;
  const nextRange = getPresetRange(preset);
  chartRange.startDate = nextRange.startDate;
  chartRange.endDate = nextRange.endDate;
  await loadChartData();
};

const applyCustomRange = async () => {
  if (!chartRange.startDate || !chartRange.endDate) {
    setFeedback("Choose both start and end dates for the chart.", "error");
    return;
  }

  if (chartRange.startDate > chartRange.endDate) {
    setFeedback("Chart start date must be before end date.", "error");
    return;
  }

  chartPreset.value = "custom";
  await loadChartData();
};

const applyTransactionFilters = async () => {
  normalizeTransactionDateFilters();

  if (transactionFilters.startDate && transactionFilters.endDate && transactionFilters.startDate > transactionFilters.endDate) {
    setFeedback("Transaction filter start date must be before end date.", "error");
    return;
  }

  transactionFilters.page = 0;
  await loadTransactions(0);
  setFeedback("Transaction filters updated.", "success");
};

const resetTransactionFilters = async () => {
  transactionFilters.accountId = "";
  transactionFilters.categoryId = "";
  transactionFilters.type = "ALL";
  transactionFilters.keyword = "";
  transactionFilters.startDate = "";
  transactionFilters.endDate = "";
  transactionFilters.page = 0;
  await loadTransactions(0);
  setFeedback("Transaction filters cleared.", "success");
};

const openEditTransaction = (transaction: Transaction) => {
  if (transaction.type === "TRANSFER") {
    setFeedback("Transfer entries can be deleted, but editing is disabled in this view.", "error");
    return;
  }

  activeWorkspaceTab.value = "activity";
  editingTransactionId.value = transaction.id;
  editTransactionForm.accountId = transaction.accountId;
  editTransactionForm.categoryId = transaction.categoryId || "";
  editTransactionForm.amount = Number(transaction.amount);
  editTransactionForm.transactionDate = transaction.transactionDate;
  editTransactionForm.note = transaction.note || "";
};

const cancelEditTransaction = () => {
  editingTransactionId.value = null;
  editTransactionForm.accountId = "";
  editTransactionForm.categoryId = "";
  editTransactionForm.amount = undefined;
  editTransactionForm.transactionDate = "";
  editTransactionForm.note = "";
};

const submitEditTransaction = async () => {
  if (!editingTransactionId.value) {
    return;
  }

  loading.value = true;
  try {
    await api.updateTransaction(editingTransactionId.value, {
      accountId: editTransactionForm.accountId,
      categoryId: editTransactionForm.categoryId,
      amount: Number(editTransactionForm.amount),
      transactionDate: editTransactionForm.transactionDate,
      note: editTransactionForm.note.trim()
    });
    await reloadAfterTransactionChange();
    cancelEditTransaction();
    setFeedback("Transaction updated.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const deleteTransaction = async (transaction: Transaction) => {
  const confirmed = import.meta.client
    ? window.confirm(`Delete ${transactionTypeLabel(transaction.type).toLowerCase()} entry on ${transaction.transactionDate}?`)
    : true;

  if (!confirmed) {
    return;
  }

  loading.value = true;
  try {
    await api.deleteTransaction(transaction.id);
    cancelEditTransaction();

    const currentPage = transactionPage.value;
    const shouldStepBack = Boolean(
      currentPage &&
      currentPage.number > 0 &&
      currentPage.content.length === 1
    );

    if (shouldStepBack) {
      transactionFilters.page = Math.max(0, transactionFilters.page - 1);
    }

    await reloadAfterTransactionChange();
    setFeedback("Transaction deleted.", "success");
  } catch (error) {
    setFeedback((error as Error).message, "error");
  } finally {
    loading.value = false;
  }
};

const goToTransactionPage = async (page: number) => {
  if (!transactionPage.value) {
    return;
  }

  if (page < 0 || page >= transactionPage.value.totalPages || page === transactionFilters.page) {
    return;
  }

  transactionFilters.page = page;
  await loadTransactions(page);
};

const logout = () => {
  api.clearAuth();
  activeWorkspaceTab.value = "overview";
  accounts.value = [];
  categories.value = [];
  transactionPage.value = null;
  budgets.value = [];
  budgetStatuses.value = [];
  summary.value = null;
  breakdown.value = [];
  cancelEditTransaction();
  resetBudgetForm();
  resetTransferForm();
  importState.accountId = "";
  importState.fileName = "";
  importState.summary = null;
  setFeedback("You have been signed out.", "success");
};

onMounted(async () => {
  api.hydrateAuth();
  accountForm.currency = api.preferredCurrency.value;

  const initialRange = getPresetRange("month");
  chartRange.startDate = initialRange.startDate;
  chartRange.endDate = initialRange.endDate;

  if (!isAuthenticated.value) {
    return;
  }

  const sessionReady = await api.ensureSession();
  if (!sessionReady) {
    setFeedback("Your session expired. Please sign in again.", "error");
    return;
  }

  try {
    await loadDashboard();
    resetCreateTransactionForm();
    resetBudgetForm();
    resetTransferForm();
    setFeedback("Workspace restored.", "success");
  } catch (error) {
    api.clearAuth();
    setFeedback((error as Error).message, "error");
  }
});
</script>

<template>
  <div class="page-shell">
    <div class="ambient ambient-left" />
    <div class="ambient ambient-right" />

    <main class="layout">
      <section class="hero-panel" :class="{ 'hero-panel--dashboard': isAuthenticated }">
        <template v-if="!isAuthenticated">
          <p class="eyebrow">Nuxt Finance Console</p>
          <h1>Build a calmer money routine with one focused ledger.</h1>
          <p class="hero-copy">
            Personal Finance Manager blends quick capture, account balance tracking, and visual cashflow reporting
            into one grounded workspace.
          </p>

          <div class="hero-metrics">
            <article>
              <span>Flow</span>
              <strong>{{ summary ? netAmount : "--" }}</strong>
              <small>Net savings snapshot</small>
            </article>
            <article>
              <span>Accounts</span>
              <strong>{{ heroAccounts }}</strong>
              <small>Connected balance pools</small>
            </article>
            <article>
              <span>Entries</span>
              <strong>{{ heroTransactions }}</strong>
              <small>Total ledger entries</small>
            </article>
          </div>
        </template>

        <template v-else>
          <div class="hero-chart-header">
            <div>
              <p class="eyebrow">Monthly Mix</p>
              <h2>Cashflow charts</h2>
            </div>
            <div class="hero-chart-actions">
              <div class="chart-period-switch" role="tablist" aria-label="Chart period">
                <button
                  class="chart-period-button"
                  :class="{ 'is-active': chartPreset === 'month' }"
                  type="button"
                  @click="applyQuickRange('month')"
                >
                  Month
                </button>
                <button
                  class="chart-period-button"
                  :class="{ 'is-active': chartPreset === 'week' }"
                  type="button"
                  @click="applyQuickRange('week')"
                >
                  Week
                </button>
                <button
                  class="chart-period-button"
                  :class="{ 'is-active': chartPreset === 'day' }"
                  type="button"
                  @click="applyQuickRange('day')"
                >
                  Day
                </button>
              </div>
              <span class="hero-chart-net">{{ netAmount }}</span>
            </div>
          </div>

          <form class="chart-range-form" @submit.prevent="applyCustomRange">
            <label class="chart-range-field">
              <span>Start date</span>
              <input v-model="chartRange.startDate" type="date" required>
            </label>
            <label class="chart-range-field">
              <span>End date</span>
              <input v-model="chartRange.endDate" type="date" required>
            </label>
            <button class="secondary-button chart-range-submit" type="submit">
              Refresh charts
            </button>
          </form>

          <p class="hero-copy hero-copy--compact">
            Compare inflow and outflow by category for the selected range, then use the ledger on the right to clean up
            any mis-categorized entries.
          </p>

          <div class="hero-chart-grid">
            <article class="chart-card">
              <header class="chart-card-header">
                <div>
                  <span class="chart-kicker">Expense</span>
                  <strong>{{ formatCurrency(breakdownByType.EXPENSE.total, api.preferredCurrency.value) }}</strong>
                </div>
                <small>{{ breakdownByType.EXPENSE.slices.length }} categories</small>
              </header>

              <div v-if="breakdownByType.EXPENSE.slices.length" class="pie-layout">
                <div class="pie-shell">
                  <div class="pie-chart" :style="{ background: breakdownByType.EXPENSE.gradient }">
                    <div class="pie-hole">
                      <span>Total</span>
                      <strong>{{ formatCurrency(breakdownByType.EXPENSE.total, api.preferredCurrency.value) }}</strong>
                    </div>
                  </div>
                </div>

                <div class="pie-legend">
                  <article v-for="item in breakdownByType.EXPENSE.slices" :key="item.key" class="legend-item">
                    <div class="legend-copy">
                      <span class="legend-dot" :style="{ backgroundColor: item.color }" />
                      <div>
                        <strong>{{ item.categoryName }}</strong>
                        <small>{{ formatCurrency(item.amount, api.preferredCurrency.value) }}</small>
                      </div>
                    </div>
                    <span class="legend-share">{{ formatShare(item.share) }}</span>
                  </article>
                </div>
              </div>

              <div v-else class="chart-empty">
                No expense data for this range yet.
              </div>
            </article>

            <article class="chart-card">
              <header class="chart-card-header">
                <div>
                  <span class="chart-kicker">Income</span>
                  <strong>{{ formatCurrency(breakdownByType.INCOME.total, api.preferredCurrency.value) }}</strong>
                </div>
                <small>{{ breakdownByType.INCOME.slices.length }} categories</small>
              </header>

              <div v-if="breakdownByType.INCOME.slices.length" class="pie-layout">
                <div class="pie-shell">
                  <div class="pie-chart" :style="{ background: breakdownByType.INCOME.gradient }">
                    <div class="pie-hole">
                      <span>Total</span>
                      <strong>{{ formatCurrency(breakdownByType.INCOME.total, api.preferredCurrency.value) }}</strong>
                    </div>
                  </div>
                </div>

                <div class="pie-legend">
                  <article v-for="item in breakdownByType.INCOME.slices" :key="item.key" class="legend-item">
                    <div class="legend-copy">
                      <span class="legend-dot" :style="{ backgroundColor: item.color }" />
                      <div>
                        <strong>{{ item.categoryName }}</strong>
                        <small>{{ formatCurrency(item.amount, api.preferredCurrency.value) }}</small>
                      </div>
                    </div>
                    <span class="legend-share">{{ formatShare(item.share) }}</span>
                  </article>
                </div>
              </div>

              <div v-else class="chart-empty">
                No income data for this range yet.
              </div>
            </article>
          </div>
        </template>
      </section>

      <section class="workspace-panel">
        <header class="workspace-header">
          <div>
            <p class="eyebrow">Connected Workspace</p>
            <h2>Personal Finance Manager</h2>
          </div>
          <button v-if="isAuthenticated" class="ghost-button" type="button" @click="logout">
            Sign out
          </button>
        </header>

        <div class="feedback" :class="feedbackTone ? `is-${feedbackTone}` : ''" aria-live="polite">
          {{ feedback }}
        </div>

        <section v-if="!isAuthenticated" class="card auth-card">
          <div class="segment-control" role="tablist" aria-label="Authentication">
            <button
              class="segment"
              :class="{ 'is-active': authMode === 'login' }"
              type="button"
              @click="authMode = 'login'"
            >
              Sign in
            </button>
            <button
              class="segment"
              :class="{ 'is-active': authMode === 'register' }"
              type="button"
              @click="authMode = 'register'"
            >
              Register
            </button>
          </div>

          <form class="form-grid" @submit.prevent="handleAuthSubmit">
            <label>
              <span>Email</span>
              <input v-model="authForm.email" type="email" placeholder="you@example.com" required>
            </label>
            <label>
              <span>Password</span>
              <input v-model="authForm.password" type="password" placeholder="At least 8 characters" required>
            </label>
            <label v-if="authMode === 'register'">
              <span>Display name</span>
              <input v-model="authForm.displayName" type="text" maxlength="100" required>
            </label>
            <label v-if="authMode === 'register'">
              <span>Currency</span>
              <input v-model="authForm.currency" type="text" maxlength="3" required>
            </label>
            <button class="primary-button" type="submit" :disabled="loading">
              {{ authMode === 'register' ? 'Create account' : 'Enter workspace' }}
            </button>
          </form>
        </section>

        <section v-else class="dashboard">
          <DashboardWorkspaceTabs
            :tabs="workspaceTabs"
            :active-tab="activeWorkspaceTab"
            :active-meta="activeWorkspaceMeta"
            @select="activeWorkspaceTab = $event"
          />

          <DashboardOverviewPanel
            v-if="activeWorkspaceTab === 'overview'"
            :summary="summary"
            :preferred-currency="api.preferredCurrency.value"
            :summary-rate="summaryRate"
            :accounts="accounts"
            :breakdown="breakdown"
            :breakdown-max="breakdownMax"
            :budget-month-label="budgetMonthLabel"
            :budget-loading="budgetLoading"
            :budget-progress-cards="budgetProgressCards"
            :format-currency="formatCurrency"
            :budget-status-class="budgetStatusClass"
          />

          <DashboardActivityPanel
            v-else-if="activeWorkspaceTab === 'activity'"
            :create-transaction-form="createTransactionForm"
            :transfer-form="transferForm"
            :transaction-filters="transactionFilters"
            :edit-transaction-form="editTransactionForm"
            :create-form-categories="createFormCategories"
            :transfer-categories="transferCategories"
            :edit-form-categories="editFormCategories"
            :accounts="accounts"
            :categories="categories"
            :transactions="transactions"
            :transaction-page="transactionPage"
            :editing-transaction-id="editingTransactionId"
            :loading="loading"
            :transaction-loading="transactionLoading"
            :transaction-result-label="transactionResultLabel"
            :amount-class="amountClass"
            :badge-class="badgeClass"
            :format-currency="formatCurrency"
            :transaction-type-label="transactionTypeLabel"
            :handle-create-transaction="handleCreateTransaction"
            :create-transfer="createTransfer"
            :apply-transaction-filters="applyTransactionFilters"
            :reset-transaction-filters="resetTransactionFilters"
            :submit-edit-transaction="submitEditTransaction"
            :cancel-edit-transaction="cancelEditTransaction"
            :open-edit-transaction="openEditTransaction"
            :delete-transaction="deleteTransaction"
            :go-to-transaction-page="goToTransactionPage"
          />

          <DashboardPlanningPanel
            v-else-if="activeWorkspaceTab === 'planning'"
            :budget-month-label="budgetMonthLabel"
            :budget-loading="budgetLoading"
            :loading="loading"
            :copy-budget-source="copyBudgetSource"
            :budget-form-title="budgetFormTitle"
            :editing-budget-id="editingBudgetId"
            :budget-form="budgetForm"
            :budget-categories="budgetCategories"
            :budgets="budgets"
            :budget-statuses="budgetProgressCards"
            :format-currency="formatCurrency"
            :format-month-value="formatMonthValue"
            :budget-status-class="budgetStatusClass"
            :preferred-currency="api.preferredCurrency.value"
            :shift-budget-month="shiftBudgetMonth"
            :copy-previous-budget-month="copyPreviousBudgetMonth"
            :save-budget="saveBudget"
            :reset-budget-form="resetBudgetForm"
            :edit-budget="editBudget"
            :remove-budget="removeBudget"
          />

          <DashboardSetupPanel
            v-else
            :account-form="accountForm"
            :category-form="categoryForm"
            :import-state="importState"
            :accounts="accounts"
            :loading="loading"
            :handle-create-account="handleCreateAccount"
            :handle-create-category="handleCreateCategory"
            :import-transactions-csv="importTransactionsCsv"
          />
        </section>
      </section>
    </main>
  </div>
</template>
