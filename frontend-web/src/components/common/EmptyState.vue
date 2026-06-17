<script setup lang="ts">
defineProps<{ message?: string }>();
</script>

<template>
  <div class="empty">
    <span class="empty-mark" aria-hidden="true">⬡</span>
    <p>{{ message ?? 'Aucune donnée à afficher.' }}</p>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  padding: 2.5rem 1rem;
  color: var(--text-muted);
}
.empty-mark {
  font-size: 1.8rem;
  color: var(--border-husk);
}
</style>
