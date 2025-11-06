import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import process from 'node:process';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

// Polyfill global, process y Buffer
import rollupNodePolyFill from 'rollup-plugin-node-polyfills';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  // By default point to Nginx (port 80) in docker-compose; override with VITE_DEV_BACKEND_URL if needed
  const target = env.VITE_DEV_BACKEND_URL || 'http://localhost';

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
        'global': 'globalthis',
        buffer: 'rollup-plugin-node-polyfills/polyfills/buffer-es6',
        process: 'rollup-plugin-node-polyfills/polyfills/process-es6',
      },
    },
    define: {
      global: 'globalThis',
    },
    build: {
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        plugins: [rollupNodePolyFill()],
        output: {
          manualChunks: {
            react: ['react', 'react-dom', 'react-router-dom'],
            gsap: ['gsap', '@gsap/react'],
            charts: ['chart.js', 'react-chartjs-2'],
          },
        },
      },
    },
    server: {
      port: 5173,
      host: '0.0.0.0',
      strictPort: true,
      proxy: {
        // Keep /api prefix, backend expects it (spring.mvc.servlet.path=/api)
        '/api': {
          target,
          changeOrigin: true,
          secure: false,
        },
        // Proxy WebSocket/SockJS endpoint during dev
        '/ws': {
          target,
          ws: true,
          changeOrigin: true,
          secure: false,
        },
      },
    },
    optimizeDeps: {
      include: ['sockjs-client', 'globalthis', 'buffer', 'process'],
      esbuildOptions: {
        define: {
          global: 'globalThis',
        },
      },
    },
  };
});
