import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      // Proxy API requests to Quarkus backend during development
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    // Ensure compatibility with older browsers if needed
    target: 'es2015',
  },
  // Optimize dependencies that need transpilation (PatternFly)
  optimizeDeps: {
    include: [
      '@patternfly/react-core',
      '@patternfly/react-table',
      '@patternfly/react-icons',
    ]
  }
});