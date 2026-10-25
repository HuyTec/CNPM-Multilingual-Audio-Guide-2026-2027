import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig, loadEnv } from 'vite';
import { resolve } from 'node:path';

export default defineConfig(({ mode }) => {
  const workspace = resolve(process.cwd(), '..');
  const app = loadEnv(mode, process.cwd(), 'API_PROXY_TARGET');
  const backend = loadEnv(mode, workspace, 'BACKEND_');
  const demoTarget = loadEnv('demo', workspace, 'API_PROXY_TARGET');
  const demoBackend = loadEnv('demo', workspace, 'BACKEND_');
  const target = app.API_PROXY_TARGET || demoTarget.API_PROXY_TARGET || `http://127.0.0.1:${backend.BACKEND_PORT || demoBackend.BACKEND_PORT || '8080'}`;
  return {
    build: { manifest: 'asset-manifest.json' },
    define: { 'import.meta.env.APP_BUILD': JSON.stringify(Date.now().toString()) },
    plugins: [react(), tailwindcss()],
    server: { host: '127.0.0.1', port: 5175, strictPort: true, proxy: { '/api': target, '/demo-audio': target } },
    preview: { host: '127.0.0.1', port: 4175, strictPort: true, proxy: { '/api': target, '/demo-audio': target } },
  };
});
