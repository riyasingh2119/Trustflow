import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // In development the API is proxied to the Spring Boot backend
    proxy: { '/api': { target: process.env.VITE_API_TARGET || 'http://localhost:8080', changeOrigin: true } },
  },
});
