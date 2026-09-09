<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const username = ref('demo');

function submit(): void {
  auth.login(username.value);
  const redirect = (route.query.redirect as string) || '/';
  void router.push(redirect);
}
</script>

<template>
  <div class="login">
    <form class="panel card" data-testid="login-form" @submit.prevent="submit">
      <div class="brand">
        <span class="mark">☕</span>
        <div>
          <div class="name">FutureKawa</div>
          <div class="eyebrow">Supervision du stockage</div>
        </div>
      </div>
      <p class="lead">Poste de supervision siège — accès consolidé multi-pays.</p>
      <label class="field">
        <span class="eyebrow">Identifiant</span>
        <input
          v-model="username"
          type="text"
          autocomplete="username"
          data-testid="login-username"
        />
      </label>
      <button class="btn btn-primary" type="submit" data-testid="login-submit">Entrer</button>
      <p class="note">
        Identification du poste de supervision. Solution déployée sur le réseau privé du siège :
        l'authentification utilisateur est hors périmètre de cette phase.
      </p>
    </form>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1rem;
  background:
    radial-gradient(1200px 600px at 70% -10%, rgba(232, 200, 154, 0.06), transparent),
    var(--bg-espresso);
}
.panel {
  width: min(380px, 100%);
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.brand {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}
.mark {
  font-size: 2rem;
}
.name {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.3rem;
}
.lead {
  color: var(--text-muted);
  font-size: 0.88rem;
  margin: 0;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.field input {
  background: var(--surface-bean);
  border: 1px solid var(--border-husk);
  border-radius: var(--radius-sm);
  color: var(--text-crema);
  padding: 0.6rem 0.7rem;
  font-family: var(--font-mono);
}
.note {
  font-size: 0.72rem;
  color: var(--text-muted);
  margin: 0;
}
</style>
