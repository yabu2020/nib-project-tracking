// vite.config.js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    // Proxy configuration for backend API calls
    proxy: {
      // Matches /api, /api/, /api/something, /api/something/else
      '^/api(/.*)?$': {
        target: 'http://localhost:8080',     // your Spring Boot backend
        changeOrigin: true,                  // changes the origin of the host header to the target URL
        secure: false,                       // for http (localhost), not https
        ws: true,                            // support WebSockets if needed later

        // Custom proxy behavior for cookies & headers
        configure: (proxy, _options) => {
          // Forward cookies from frontend → backend
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            if (req.headers.cookie) {
              proxyReq.setHeader('Cookie', req.headers.cookie);
            }
          });

          // Forward Set-Cookie from backend → frontend
          proxy.on('proxyRes', (proxyRes, _req, res) => {
            const setCookie = proxyRes.headers['set-cookie'];
            if (setCookie) {
              res.setHeader('Set-Cookie', setCookie);
            }
          });
        },

        // Optional: rewrite path if your backend expects different prefix
        // (usually not needed if backend endpoints start with /api)
        // rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
    },

    // Optional: automatically open browser on dev server start
    open: true,

    // Optional: custom port if 5173 conflicts
    // port: 5173,
  },
})