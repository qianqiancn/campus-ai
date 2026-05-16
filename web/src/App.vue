<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
:root {
  --color-primary: #6366f1;
  --color-primary-light: #818cf8;
  --color-primary-dark: #4f46e5;
  --color-accent: #8b5cf6;
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;
  --color-info: #6366f1;

  --color-bg: #f8f9fc;
  --color-surface: #ffffff;
  --color-border: #e8eaf0;
  --color-border-light: #f1f3f8;

  --color-text-primary: #111827;
  --color-text-secondary: #4b5563;
  --color-text-tertiary: #9ca3af;
  --color-text-inverse: #ffffff;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.06), 0 2px 4px -2px rgba(0, 0, 0, 0.04);
  --shadow-lg: 0 10px 25px -5px rgba(0, 0, 0, 0.07), 0 4px 10px -6px rgba(0, 0, 0, 0.04);
  --shadow-xl: 0 20px 40px -10px rgba(0, 0, 0, 0.08);

  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 24px;

  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 250ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 400ms cubic-bezier(0.4, 0, 0.2, 1);

  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

[data-theme="dark"] {
  --color-primary: #818cf8;
  --color-primary-light: #a5b4fc;
  --color-primary-dark: #6366f1;
  --color-accent: #a78bfa;
  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-danger: #f87171;
  --color-info: #818cf8;

  --color-bg: #0c0e1a;
  --color-surface: #161829;
  --color-border: #252840;
  --color-border-light: #1c1e34;

  --color-text-primary: #eef0f6;
  --color-text-secondary: #9ca3af;
  --color-text-tertiary: #6b7280;
  --color-text-inverse: #0c0e1a;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.3);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4), 0 2px 4px -2px rgba(0, 0, 0, 0.3);
  --shadow-lg: 0 10px 25px -5px rgba(0, 0, 0, 0.5), 0 4px 10px -6px rgba(0, 0, 0, 0.4);
  --shadow-xl: 0 20px 40px -10px rgba(0, 0, 0, 0.6);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  font-family: var(--font-sans);
  background-color: var(--color-bg);
  color: var(--color-text-primary);
  line-height: 1.6;
}

#app {
  min-height: 100vh;
}

::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

[data-theme="dark"] ::-webkit-scrollbar-thumb {
  background: #374151;
}

[data-theme="dark"] ::-webkit-scrollbar-thumb:hover {
  background: #4b5563;
}
</style>
