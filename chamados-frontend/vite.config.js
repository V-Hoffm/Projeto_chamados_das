import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  // `npm run build` gera os arquivos direto na pasta de recursos estáticos do Spring Boot.
  build: { outDir: '../chamados/src/main/resources/static', emptyOutDir: true },
});
