<script setup lang="ts">
import type { Account, Category, Transaction, TransactionPage } from "../../types/api";
import type {
  CreateTransactionFormState,
  EditTransactionFormState,
  TransactionFiltersState,
  TransferFormState
} from "../../types/dashboard";

defineProps<{
  createTransactionForm: CreateTransactionFormState;
  transferForm: TransferFormState;
  transactionFilters: TransactionFiltersState;
  editTransactionForm: EditTransactionFormState;
  createFormCategories: Category[];
  transferCategories: Category[];
  editFormCategories: Category[];
  accounts: Account[];
  categories: Category[];
  transactions: Transaction[];
  transactionPage: TransactionPage | null;
  editingTransactionId: string | null;
  loading: boolean;
  transactionLoading: boolean;
  transactionResultLabel: string;
  amountClass: (type: string) => string;
  badgeClass: (type: string) => string;
  formatCurrency: (value: number, currency: string) => string;
  transactionTypeLabel: (type: string) => string;
  handleCreateTransaction: () => void;
  createTransfer: () => void;
  applyTransactionFilters: () => void;
  resetTransactionFilters: () => void;
  submitEditTransaction: () => void;
  cancelEditTransaction: () => void;
  openEditTransaction: (transaction: Transaction) => void;
  deleteTransaction: (transaction: Transaction) => void;
  goToTransactionPage: (page: number) => void;
}>();
</script>

<template>
  <div class="dashboard-grid">
    <section class="card stack-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Quick capture</p>
          <h3>Add transaction</h3>
        </div>
      </div>

      <form class="form-grid compact-grid" @submit.prevent="handleCreateTransaction">
        <label>
          <span>Type</span>
          <select v-model="createTransactionForm.type">
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </select>
        </label>
        <label>
          <span>Account</span>
          <select v-model="createTransactionForm.accountId" required>
            <option v-for="account in accounts" :key="account.id" :value="account.id">
              {{ account.name }} · {{ account.currency }}
            </option>
          </select>
        </label>
        <label>
          <span>Category</span>
          <select v-model="createTransactionForm.categoryId" required>
            <option v-for="category in createFormCategories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>
        <label>
          <span>Amount</span>
          <input v-model="createTransactionForm.amount" type="number" min="0.01" step="0.01" required>
        </label>
        <label>
          <span>Date</span>
          <input v-model="createTransactionForm.transactionDate" type="date" required>
        </label>
        <label class="full-span">
          <span>Note</span>
          <input v-model="createTransactionForm.note" type="text" maxlength="1000" placeholder="Optional details">
        </label>
        <button class="primary-button" type="submit" :disabled="loading || !accounts.length || !createFormCategories.length">
          Save transaction
        </button>
      </form>
    </section>

    <section class="card stack-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Movement</p>
          <h3>Record transfer</h3>
        </div>
      </div>

      <form class="form-grid compact-grid" @submit.prevent="createTransfer">
        <label>
          <span>From account</span>
          <select v-model="transferForm.fromAccountId" required>
            <option v-for="account in accounts" :key="`from-${account.id}`" :value="account.id">
              {{ account.name }} · {{ account.currency }}
            </option>
          </select>
        </label>
        <label>
          <span>To account</span>
          <select v-model="transferForm.toAccountId" required>
            <option v-for="account in accounts" :key="`to-${account.id}`" :value="account.id">
              {{ account.name }} · {{ account.currency }}
            </option>
          </select>
        </label>
        <label>
          <span>Category</span>
          <select v-model="transferForm.categoryId" required>
            <option v-for="category in transferCategories" :key="`transfer-${category.id}`" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>
        <label>
          <span>Amount</span>
          <input v-model="transferForm.amount" type="number" min="0.01" step="0.01" required>
        </label>
        <label>
          <span>Date</span>
          <input v-model="transferForm.transactionDate" type="date" required>
        </label>
        <label class="full-span">
          <span>Note</span>
          <input v-model="transferForm.note" type="text" maxlength="1000" placeholder="Optional transfer memo">
        </label>
        <button class="secondary-button" type="submit" :disabled="loading || accounts.length < 2 || !transferCategories.length">
          Save transfer
        </button>
      </form>
    </section>

    <section class="card ledger-card full-width">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Ledger</p>
          <h3>Search, edit, and delete transactions</h3>
        </div>
        <span class="section-meta">{{ transactionResultLabel }}</span>
      </div>

      <form class="form-grid compact-grid filter-grid" @submit.prevent="applyTransactionFilters">
        <label>
          <span>Keyword</span>
          <input v-model="transactionFilters.keyword" type="text" placeholder="Search note text">
        </label>
        <label>
          <span>Type</span>
          <select v-model="transactionFilters.type">
            <option value="ALL">All</option>
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
            <option value="TRANSFER">Transfer</option>
          </select>
        </label>
        <label>
          <span>Account</span>
          <select v-model="transactionFilters.accountId">
            <option value="">All accounts</option>
            <option v-for="account in accounts" :key="account.id" :value="account.id">
              {{ account.name }}
            </option>
          </select>
        </label>
        <label>
          <span>Category</span>
          <select v-model="transactionFilters.categoryId">
            <option value="">All categories</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </label>
        <label>
          <span>Start date</span>
          <input
            v-model="transactionFilters.startDate"
            type="date"
            :max="transactionFilters.endDate || undefined"
          >
        </label>
        <label>
          <span>End date</span>
          <input
            v-model="transactionFilters.endDate"
            type="date"
            :min="transactionFilters.startDate || undefined"
          >
        </label>
        <div class="filter-actions full-span">
          <button class="secondary-button" type="submit" :disabled="transactionLoading">Apply filters</button>
          <button class="ghost-button" type="button" :disabled="transactionLoading" @click="resetTransactionFilters">
            Clear filters
          </button>
        </div>
      </form>

      <div class="list-shell ledger-list">
        <article v-if="transactionLoading" class="list-item">
          <span>Loading transactions...</span>
        </article>
        <article v-else-if="!transactions.length" class="list-item">
          <span>No transactions match the current filters.</span>
        </article>

        <article v-for="transaction in transactions" :key="transaction.id" class="list-item list-item--transaction">
          <template v-if="editingTransactionId === transaction.id">
            <form class="transaction-edit-form" @submit.prevent="submitEditTransaction">
              <div class="transaction-headline">
                <span :class="badgeClass(transaction.type)">{{ transactionTypeLabel(transaction.type) }}</span>
                <strong>Edit ledger entry</strong>
              </div>

              <div class="form-grid compact-grid">
                <label>
                  <span>Account</span>
                  <select v-model="editTransactionForm.accountId" required>
                    <option v-for="account in accounts" :key="account.id" :value="account.id">
                      {{ account.name }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>Category</span>
                  <select v-model="editTransactionForm.categoryId" required>
                    <option v-for="category in editFormCategories" :key="category.id" :value="category.id">
                      {{ category.name }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>Amount</span>
                  <input v-model="editTransactionForm.amount" type="number" min="0.01" step="0.01" required>
                </label>
                <label>
                  <span>Date</span>
                  <input v-model="editTransactionForm.transactionDate" type="date" required>
                </label>
                <label class="full-span">
                  <span>Note</span>
                  <input v-model="editTransactionForm.note" type="text" maxlength="1000">
                </label>
              </div>

              <div class="filter-actions">
                <button class="primary-button" type="submit" :disabled="loading">Save changes</button>
                <button class="ghost-button" type="button" :disabled="loading" @click="cancelEditTransaction">
                  Cancel
                </button>
              </div>
            </form>
          </template>

          <template v-else>
            <div class="transaction-summary">
              <div class="transaction-headline">
                <span :class="badgeClass(transaction.type)">{{ transactionTypeLabel(transaction.type) }}</span>
                <strong>{{ transaction.categoryName || "Uncategorized" }}</strong>
              </div>
              <span>{{ transaction.transactionDate }} · {{ transaction.accountName }}</span>
              <small v-if="transaction.note">{{ transaction.note }}</small>
            </div>

            <div class="transaction-actions">
              <div class="transaction-amount">
                <span>{{ transaction.currency }}</span>
                <strong :class="amountClass(transaction.type)">
                  {{ transaction.type === 'EXPENSE' ? '-' : '+' }}{{ formatCurrency(transaction.amount, transaction.currency) }}
                </strong>
              </div>
              <div class="transaction-buttons">
                <button
                  class="secondary-button"
                  type="button"
                  :disabled="loading || transaction.type === 'TRANSFER'"
                  @click="openEditTransaction(transaction)"
                >
                  Edit
                </button>
                <button class="ghost-button danger-button" type="button" :disabled="loading" @click="deleteTransaction(transaction)">
                  Delete
                </button>
              </div>
            </div>
          </template>
        </article>
      </div>

      <div v-if="transactionPage && transactionPage.totalPages > 1" class="pagination-bar">
        <button
          class="ghost-button"
          type="button"
          :disabled="transactionPage.first || transactionLoading"
          @click="goToTransactionPage(transactionPage.number - 1)"
        >
          Previous
        </button>
        <span>Page {{ transactionPage.number + 1 }} / {{ transactionPage.totalPages }}</span>
        <button
          class="ghost-button"
          type="button"
          :disabled="transactionPage.last || transactionLoading"
          @click="goToTransactionPage(transactionPage.number + 1)"
        >
          Next
        </button>
      </div>
    </section>
  </div>
</template>
