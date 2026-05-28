<script setup lang="ts">
import type { Account } from "../../types/api";
import type { ImportState } from "../../types/dashboard";

defineProps<{
  accountForm: {
    name: string;
    type: string;
    currency: string;
    initialBalance: number;
    note: string;
  };
  categoryForm: {
    name: string;
    type: "EXPENSE" | "INCOME" | "BOTH";
    icon: string;
    color: string;
  };
  importState: ImportState;
  accounts: Account[];
  loading: boolean;
  handleCreateAccount: () => void;
  handleCreateCategory: () => void;
  importTransactionsCsv: (event: Event) => void;
}>();
</script>

<template>
  <div class="dashboard-grid">
    <section class="card stack-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Structure</p>
          <h3>Accounts & categories</h3>
        </div>
      </div>

      <form class="form-grid compact-grid form-block" @submit.prevent="handleCreateAccount">
        <label>
          <span>Account name</span>
          <input v-model="accountForm.name" type="text" maxlength="100" required>
        </label>
        <label>
          <span>Type</span>
          <select v-model="accountForm.type">
            <option value="BANK">BANK</option>
            <option value="CASH">CASH</option>
            <option value="CREDIT_CARD">CREDIT_CARD</option>
            <option value="E_WALLET">E_WALLET</option>
            <option value="INVESTMENT">INVESTMENT</option>
            <option value="OTHER">OTHER</option>
          </select>
        </label>
        <label>
          <span>Currency</span>
          <input v-model="accountForm.currency" type="text" maxlength="3" required>
        </label>
        <label>
          <span>Initial balance</span>
          <input v-model="accountForm.initialBalance" type="number" step="0.01" required>
        </label>
        <label class="full-span">
          <span>Note</span>
          <input v-model="accountForm.note" type="text" maxlength="1000">
        </label>
        <button class="secondary-button" type="submit" :disabled="loading">Create account</button>
      </form>

      <form class="form-grid compact-grid form-block" @submit.prevent="handleCreateCategory">
        <label>
          <span>Category name</span>
          <input v-model="categoryForm.name" type="text" maxlength="100" required>
        </label>
        <label>
          <span>Type</span>
          <select v-model="categoryForm.type">
            <option value="EXPENSE">EXPENSE</option>
            <option value="INCOME">INCOME</option>
            <option value="BOTH">BOTH</option>
          </select>
        </label>
        <label>
          <span>Icon</span>
          <input v-model="categoryForm.icon" type="text" maxlength="100" placeholder="fork-knife">
        </label>
        <label>
          <span>Color</span>
          <input v-model="categoryForm.color" type="text" maxlength="7" required>
        </label>
        <button class="secondary-button" type="submit" :disabled="loading">Create category</button>
      </form>
    </section>

    <section class="card stack-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Import</p>
          <h3>CSV intake</h3>
        </div>
      </div>

      <div class="import-copy">
        <p>Choose the target account, then upload a UTF-8 CSV with columns: <code>type,categoryName,amount,transactionDate,note</code>.</p>
        <small>Dates should use <code>YYYY-MM-DD</code>. Supported type values: <code>INCOME</code>, <code>EXPENSE</code>.</small>
      </div>

      <label>
        <span>Import into account</span>
        <select v-model="importState.accountId" :disabled="loading" required>
          <option v-for="account in accounts" :key="`import-${account.id}`" :value="account.id">
            {{ account.name }} · {{ account.currency }}
          </option>
        </select>
      </label>

      <label class="upload-field">
        <span>Select CSV file</span>
        <input type="file" accept=".csv,text/csv" :disabled="loading" @change="importTransactionsCsv">
      </label>

      <div v-if="importState.fileName" class="import-summary">
        <strong>{{ importState.fileName }}</strong>
        <span v-if="importState.summary">
          {{ importState.summary.successCount }} imported · {{ importState.summary.failedCount }} failed
        </span>
      </div>

      <div v-if="importState.summary?.errors?.length" class="import-errors">
        <strong>Import errors</strong>
        <ul>
          <li v-for="errorMessage in importState.summary.errors" :key="errorMessage">{{ errorMessage }}</li>
        </ul>
      </div>
    </section>
  </div>
</template>
