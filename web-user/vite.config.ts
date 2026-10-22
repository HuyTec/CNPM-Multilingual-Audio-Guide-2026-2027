import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const target = loadEnv(mode, process.cwd(), '').API_PROXY_TARGET || 'http://127.0.0.1:8080';
  return {
    define: { 'import.meta.env.APP_BUILD': JSON.stringify(Date.now().toString()) },
    plugins: [react(), tailwindcss()],
    server: { host: '127.0.0.1', port: 5175, strictPort: true, proxy: { '/api': target, '/demo-audio': target } },
    preview: { host: '127.0.0.1', port: 4175, strictPort: true, proxy: { '/api': target, '/demo-audio': target } },
  };
});
