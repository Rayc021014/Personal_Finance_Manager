<script setup lang="ts">
import type { WorkspaceTab, WorkspaceTabItem } from "../../types/dashboard";

defineProps<{
  tabs: WorkspaceTabItem[];
  activeTab: WorkspaceTab;
  activeMeta: WorkspaceTabItem;
}>();

const emit = defineEmits<{
  (event: "select", tab: WorkspaceTab): void;
}>();
</script>

<template>
  <div class="dashboard-tabs">
    <nav class="workspace-nav" aria-label="Workspace sections">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="workspace-nav-item"
        :class="{ 'is-active': activeTab === tab.id }"
        type="button"
        @click="emit('select', tab.id)"
      >
        <span class="workspace-nav-eyebrow">{{ tab.eyebrow }}</span>
        <strong>{{ tab.label }}</strong>
        <span class="workspace-nav-copy">{{ tab.description }}</span>
        <span class="workspace-nav-badge">{{ tab.badge }}</span>
      </button>
    </nav>

    <section class="card tab-intro-card">
      <div class="section-heading">
        <div>
          <p class="eyebrow">{{ activeMeta.eyebrow }}</p>
          <h3>{{ activeMeta.label }}</h3>
        </div>
        <span class="section-meta">{{ activeMeta.badge }}</span>
      </div>
      <p class="hero-copy hero-copy--compact tab-intro-copy">{{ activeMeta.description }}</p>
    </section>
  </div>
</template>
