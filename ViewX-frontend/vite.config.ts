import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  define: {
    // Fix for sockjs-client: provide global variable
    global: 'globalThis'
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Don't rewrite since backend has context-path: /api
        // The request /api/xxx will be proxied to http://localhost:8080/api/xxx
      },
      '/rabbitmq-api': {
        target: 'http://localhost:15672',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/rabbitmq-api/, '/api')
      }
    }
  }
})