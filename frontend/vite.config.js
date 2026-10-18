import { defineConfig } from 'vite';
export default defineConfig({
  server: { port: 5173, strictPort: true, proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } } },
  build: { target: 'es2022', sourcemap: false, chunkSizeWarningLimit: 600 },
  esbuild: { jsxFactory: 'React.createElement', jsxFragment: 'React.Fragment' },
});
