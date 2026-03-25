import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 3006,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'esnext',
    rollupOptions: {
      output: {
        manualChunks: {
          'lit': ['lit'],
          'patternfly': ['@patternfly/elements'],
        },
      },
    },
  },
});

// Made with Bob
