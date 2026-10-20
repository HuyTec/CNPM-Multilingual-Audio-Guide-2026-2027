import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const target =
    loadEnv(mode, process.cwd(), "").API_PROXY_TARGET ||
    "http://127.0.0.1:8080";
  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: "127.0.0.1",
      proxy: { "/api": target, "/demo-audio": target },
    },
  };
});
