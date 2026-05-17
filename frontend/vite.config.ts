import { defineConfig } from "vite";
import { resolve } from "path";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [
      react(),
      tailwindcss()
  ],
<<<<<<< HEAD
  build: {
    outDir: '../social-manager-backend/src/main/resources/static',
    emptyOutDir: true,
  },
=======
>>>>>>> upstream/dev
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "src/")
    }
  }
})
