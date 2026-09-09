import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import { router } from './router';
import { registerCoffeeTheme } from './lib/echarts';
import './styles/base.css';

async function bootstrap(): Promise<void> {
  if (import.meta.env.VITE_USE_MOCKS === 'true') {
    const { worker } = await import('./mocks/browser');
    await worker.start({ onUnhandledRequest: 'bypass' });
  }

  registerCoffeeTheme();

  const app = createApp(App);
  app.use(createPinia());
  app.use(router);
  app.mount('#app');
}

void bootstrap();
