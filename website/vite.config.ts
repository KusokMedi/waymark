import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const base = process.env.VITE_BASE_PATH ?? "/waymark/";

export default defineConfig({
  base,
  plugins: [react()],
  server: { port: 5174 },
  build: { outDir: "dist", sourcemap: true },
});