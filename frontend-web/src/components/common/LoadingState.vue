<template>
  <div class="loading">
    <span class="bean" aria-hidden="true">☕</span>
    <span>Chargement…</span>
  </div>
</template>

<style scoped>
.loading {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 2rem 1rem;
  color: var(--text-muted);
}
.bean {
  animation: pulse 1.4s ease-in-out infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 0.4;
  }
  50% {
    opacity: 1;
  }
}
</style>
