<script setup lang="ts">
import type { Account, CategoryBreakdownItem, Summary } from "../../types/api";
import type { BudgetProgressCard } from "../../types/dashboard";

defineProps<{
  summary: Summary | null;
  preferredCurrency: string;
  summaryRate: string;
  accounts: Account[];
  breakdown: CategoryBreakdownItem[];
  breakdownMax: number;
  budgetMonthLabel: string;
  budgetLoading: boolean;
  budgetProgressCards: BudgetProgressCard[];
  formatCurrency: (value: number, currency: string) => string;
  budgetStatusClass: (status: string) => string;
}>();
</script>

<template>
  <div class="overview-panel">
    <div class="summary-grid">
      <article class="card stat-card">
        <span>Total income</span>
        <strong>{{ summary ? formatCurrency(summary.totalIncome, preferredCurrency) : "--" }}</strong>
      </article>
      <article class="card stat-card">
        <span>Total expense</span>
        <strong>{{ summary ? formatCurrency(summary.totalExpense, preferredCurrency) : "--" }}</strong>
      </article>
      <article class="card stat-card">
        <span>Net savings</span>
        <strong>{{ summary ? formatCurrency(summary.netSavings, preferredCurrency) : "--" }}</strong>
      </article>
      <article class="card stat-card">
        <span>Savings rate</span>
        <strong>{{ summary ? summaryRate : "--" }}</strong>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="card ledger-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Overview</p>
            <h3>Account balances</h3>
          </div>
        </div>
        <div class="list-shell">
          <article v-if="!accounts.length" class="list-item">
            <span>Create your first account to start tracking balances.</span>
          </article>
          <article v-for="account in accounts" :key="account.id" class="list-item">
            <div>
              <span>{{ account.type }}</span>
              <strong>{{ account.name }}</strong>
            </div>
            <div>
              <span>{{ account.currency }}</span>
              <strong>{{ formatCurrency(account.currentBalance, account.currency) }}</strong>
            </div>
          </article>
        </div>
      </section>

      <section class="card ledger-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Breakdown</p>
            <h3>Category totals</h3>
          </div>
        </div>
        <div class="breakdown-shell">
          <article v-if="!breakdown.length" class="list-item">
            <span>No category totals yet for the selected chart range.</span>
          </article>
          <article v-for="item in breakdown" :key="`${item.type}-${item.categoryId || item.categoryName}`" class="breakdown-row">
            <header>
              <span>{{ item.categoryName }}</span>
              <strong>{{ formatCurrency(item.amount, preferredCurrency) }}</strong>
            </header>
            <div class="breakdown-bar">
              <div class="breakdown-fill" :style="{ width: `${(item.amount / breakdownMax) * 100}%` }" />
            </div>
          </article>
        </div>
      </section>

      <section class="card ledger-card full-width">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Budgets</p>
            <h3>Spending progress</h3>
          </div>
          <span class="section-meta">{{ budgetMonthLabel }}</span>
        </div>

        <div class="budget-status-list">
          <article v-if="budgetLoading" class="list-item">
            <span>Loading budget status...</span>
          </article>
          <article v-else-if="!budgetProgressCards.length" class="list-item">
            <span>Create a budget to start monitoring spend.</span>
          </article>
          <article v-for="item in budgetProgressCards" :key="item.budgetId" class="budget-status-card">
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
  </div>
</template>
