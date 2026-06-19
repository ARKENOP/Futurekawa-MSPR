<script setup lang="ts">
const props = defineProps<{ page: number; totalPages: number; totalElements: number }>();
const emit = defineEmits<{ 'update:page': [value: number] }>();

function go(p: number): void {
  if (p >= 0 && p < props.totalPages) emit('update:page', p);
}
</script>

<template>
  <div class="pagination">
    <span class="count mono">{{ totalElements }} élément(s)</span>
    <div class="controls">
      <button class="btn" :disabled="page === 0" @click="go(page - 1)">‹ Préc.</button>
      <span class="mono page-ind">{{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn" :disabled="page >= totalPages - 1" @click="go(page + 1)">Suiv. ›</button>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.9rem;
  font-size: 0.82rem;
  color: var(--text-muted);
}
.controls {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-ind {
  color: var(--text-crema);
}
</style>
