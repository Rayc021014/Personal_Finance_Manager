<script setup lang="ts">
import type { Budget, Category } from "../../types/api";
import type { BudgetFormState, BudgetProgressCard } from "../../types/dashboard";

defineProps<{
  budgetMonthLabel: string;
  budgetLoading: boolean;
  loading: boolean;
  copyBudgetSource: string;
  budgetFormTitle: string;
  editingBudgetId: string | null;
  budgetForm: BudgetFormState;
  budgetCategories: Category[];
  budgets: Budget[];
  budgetStatuses: BudgetProgressCard[];
  formatCurrency: (value: number, currency: string) => string;
  formatMonthValue: (year: number, month: number) => string;
  budgetStatusClass: (status: string) => string;
  preferredCurrency: string;
  shiftBudgetMonth: (offset: number) => void;
  copyPreviousBudgetMonth: () => void;
  saveBudget: () => void;
  resetBudgetForm: () => void;
  editBudget: (budget: Budget) => void;
  removeBudget: (budget: Budget) => void;
}>();
</script>

<template>
  <div class="dashboard-grid">
    <section class="card ledger-card full-width">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Budgets</p>
          <h3>Monthly guardrails</h3>
        </div>
        <span class="section-meta">{{ budgetMonthLabel }}</span>
      </div>

      <div class="budget-toolbar">
        <div class="budget-nav">
          <button class="ghost-button" type="button" :disabled="budgetLoading" @click="shiftBudgetMonth(-1)">
            Previous month
          </button>
          <button class="ghost-button" type="button" :disabled="budgetLoading" @click="shiftBudgetMonth(1)">
            Next month
          </button>
        </div>
        <button class="secondary-button" type="button" :disabled="loading || budgetLoading" @click="copyPreviousBudgetMonth">
          Copy from {{ copyBudgetSource }}
        </button>
      </div>

      <div class="budget-layout">
        <section class="budget-panel">
          <header class="budget-panel-header">
            <div>
              <span class="chart-kicker">Planner</span>
              <strong>{{ budgetFormTitle }}</strong>
            </div>
            <small>{{ budgets.length }} budgets in this month</small>
          </header>

          <form class="form-grid compact-grid" @submit.prevent="saveBudget">
            <label>
              <span>Category</span>
              <select v-model="budgetForm.categoryId" required>
                <option v-for="category in budgetCategories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </label>
            <label>
              <span>Limit</span>
              <input v-model="budgetForm.amountLimit" type="number" min="0.01" step="0.01" required>
            </label>
            <div class="filter-actions full-span">
              <button class="primary-button" type="submit" :disabled="loading || !budgetCategories.length">
                {{ editingBudgetId ? 'Update budget' : 'Create budget' }}
              </button>
              <button v-if="editingBudgetId" class="ghost-button" type="button" :disabled="loading" @click="resetBudgetForm">
                Cancel edit
              </button>
            </div>
          </form>

          <div class="list-shell budget-list">
            <article v-if="budgetLoading" class="list-item">
              <span>Loading budgets...</span>
            </article>
            <article v-else-if="!budgets.length" class="list-item">
              <span>No budgets yet for {{ budgetMonthLabel }}.</span>
            </article>
            <article v-for="budget in budgets" :key="budget.id" class="list-item list-item--transaction">
              <div class="transaction-summary">
                <div class="transaction-headline">
                  <strong>{{ budget.categoryName }}</strong>
                </div>
                <span>{{ formatMonthValue(budget.year, budget.month) }}</span>
              </div>
              <div class="transaction-actions">
                <div class="transaction-amount">
                  <span>Limit</span>
                  <strong>{{ formatCurrency(budget.amountLimit, preferredCurrency) }}</strong>
                </div>
                <div class="transaction-buttons">
                  <button class="secondary-button" type="button" :disabled="loading" @click="editBudget(budget)">
                    Edit
                  </button>
                  <button class="ghost-button danger-button" type="button" :disabled="loading" @click="removeBudget(budget)">
                    Delete
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="budget-panel">
          <header class="budget-panel-header">
            <div>
              <span class="chart-kicker">Status</span>
              <strong>Spend against plan</strong>
            </div>
            <small>{{ budgetStatuses.length }} tracked categories</small>
          </header>

          <div class="budget-status-list">
            <article v-if="budgetLoading" class="list-item">
              <span>Loading budget status...</span>
            </article>
            <article v-else-if="!budgetStatuses.length" class="list-item">
              <span>Create a budget to start monitoring spend.</span>
            </article>
            <article v-for="item in budgetStatuses" :key="item.budgetId" class="budget-status-card">
              <div class="budget-status-top">
                <div>
                  <strong>{{ item.categoryName }}</strong>
                  <span>{{ formatCurrency(item.spentAmount, preferredCurrency) }} spent</span>
                </div>
                <span :class="budgetStatusClass(item.status)">{{ item.status }}</span>
              </div>
              <div class="budget-progress-rail">
                <div
                  class="budget-progress-fill"
                  :class="budgetStatusClass(item.status)"
                  :style="{ width: `${item.clampedRate * 100}%` }"
                />
              </div>
              <div class="budget-status-meta">
                <span>{{ formatCurrency(item.amountLimit, preferredCurrency) }} limit</span>
                <strong :class="item.remaining < 0 ? 'amount-negative' : 'amount-positive'">
                  {{ item.remaining < 0 ? 'Over by ' : 'Remaining ' }}{{ formatCurrency(Math.abs(item.remaining), preferredCurrency) }}
                </strong>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>
