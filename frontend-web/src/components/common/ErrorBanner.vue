<script setup lang="ts">
defineProps<{ message: string }>();
defineEmits<{ retry: [] }>();
</script>

<template>
  <div class="error" role="alert" data-testid="error-banner">
    <span class="mark" aria-hidden="true">⚠</span>
    <p class="msg">{{ message }}</p>
    <button class="btn small" @click="$emit('retry')">Réessayer</button>
  </div>
</template>

<style scoped>
.error {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.9rem;
  margin-bottom: 1rem;
  border: 1px solid var(--niveau-critique);
  border-left: 3px solid var(--niveau-critique);
  border-radius: var(--radius-sm);
  background: var(--surface-bean);
  font-size: 0.86rem;
}
.mark {
  color: var(--niveau-critique);
  font-size: 1.1rem;
}
.msg {
  flex: 1;
  margin: 0;
}
</style>
