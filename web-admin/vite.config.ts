import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";
import tailwindcss from "@tailwindcss/vite";
import { resolve } from "node:path";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const workspace = resolve(process.cwd(), "..");
  const app = loadEnv(mode, process.cwd(), "API_PROXY_TARGET");
  const demoTarget = loadEnv("demo", workspace, "API_PROXY_TARGET");
  const demoBackend = loadEnv("demo", workspace, "BACKEND_");
  const target =
    app.API_PROXY_TARGET ||
    demoTarget.API_PROXY_TARGET ||
    `http://127.0.0.1:${demoBackend.BACKEND_PORT || "8080"}`;
  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: "127.0.0.1",
      proxy: { "/api": target, "/demo-audio": target },
    },
  };
});
