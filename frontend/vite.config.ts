import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// 로컬 개발 서버(npm run dev)의 /api 프록시.
// 프로덕션에서는 nginx(docker/nginx/default.conf)가 동일한 경로 분배를 담당한다.
// 각 백엔드를 로컬에서 직접 띄울 때의 기본 포트로 라우팅한다.
const BLOG = process.env.VITE_BLOG_API ?? 'http://localhost:8080'
const AUTH = process.env.VITE_AUTH_API ?? 'http://localhost:8082'
const CHAT = process.env.VITE_CHAT_API ?? 'http://localhost:8081'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  define: {
    global: 'globalThis',
  },
  server: {
    proxy: {
      // auth-service
      '/api/auth': { target: AUTH, changeOrigin: true },
      '/api/users': { target: AUTH, changeOrigin: true },
      '/api/admin': { target: AUTH, changeOrigin: true },
      // chat-service
      '/api/chat-rooms': { target: CHAT, changeOrigin: true },
      '/api/messages': { target: CHAT, changeOrigin: true },
      '/api/service-info': { target: CHAT, changeOrigin: true },
      '/ws': { target: CHAT, changeOrigin: true, ws: true },
      // blog-service (posts/todos) — 기본
      '/api': { target: BLOG, changeOrigin: true },
    },
  },
})
