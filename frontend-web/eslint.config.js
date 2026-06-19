import pluginVue from 'eslint-plugin-vue';
import vueTsEslintConfig from '@vue/eslint-config-typescript';
import prettier from '@vue/eslint-config-prettier';

export default [
  { ignores: ['dist', 'node_modules', 'public/mockServiceWorker.js'] },
  ...pluginVue.configs['flat/recommended'],
  ...vueTsEslintConfig(),
  prettier,
];
