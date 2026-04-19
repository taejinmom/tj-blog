import axios, { type AxiosRequestConfig } from 'axios';
import type { Post, PostRequest, TodoItem, TodoRequest } from '../types';

const api = axios.create({
  baseURL: '/api',
});

export const postApi = {
  getAll: () => api.get<Post[]>('/posts').then(r => r.data),
  getById: (id: number) => api.get<Post>(`/posts/${id}`).then(r => r.data),
  getByCategory: (category: string) => api.get<Post[]>(`/posts/category/${category}`).then(r => r.data),
  create: (data: PostRequest) => api.post<Post>('/posts', data).then(r => r.data),
  update: (id: number, data: PostRequest) => api.put<Post>(`/posts/${id}`, data).then(r => r.data),
  delete: (id: number) => api.delete(`/posts/${id}`),
};

export const todoApi = {
  getAll: () => api.get<TodoItem[]>('/todos').then(r => r.data),
  getById: (id: number) => api.get<TodoItem>(`/todos/${id}`).then(r => r.data),
  getByPhase: (phase: string) => api.get<TodoItem[]>(`/todos/phase/${phase}`).then(r => r.data),
  getByStatus: (status: string) => api.get<TodoItem[]>(`/todos/status/${status}`).then(r => r.data),
  create: (data: TodoRequest) => api.post<TodoItem>('/todos', data).then(r => r.data),
  update: (id: number, data: TodoRequest) => api.put<TodoItem>(`/todos/${id}`, data).then(r => r.data),
  delete: (id: number) => api.delete(`/todos/${id}`),
};

// --- JWT auth interceptors ---

type RetriableConfig = AxiosRequestConfig & { _retry?: boolean };

let isRefreshing = false;
let refreshQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = (error.config ?? {}) as RetriableConfig;
    const url: string = original?.url ?? '';

    const isAuthEndpoint =
      url.includes('/auth/login') ||
      url.includes('/auth/signup') ||
      url.includes('/auth/refresh');

    if (
      error.response?.status !== 401 ||
      original._retry ||
      isAuthEndpoint
    ) {
      return Promise.reject(error);
    }

    // If a refresh is already in flight, queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({
          resolve: (token: string) => {
            original.headers = original.headers ?? {};
            (original.headers as Record<string, string>).Authorization = `Bearer ${token}`;
            resolve(api(original));
          },
          reject,
        });
      });
    }

    original._retry = true;
    isRefreshing = true;

    try {
      const refreshToken = localStorage.getItem('refreshToken');
      // Use a bare axios call (NOT `api`) to avoid recursion through interceptors.
      const { data } = await axios.post('/api/auth/refresh', { refreshToken });

      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);

      refreshQueue.forEach(({ resolve }) => resolve(data.accessToken));
      refreshQueue = [];

      original.headers = original.headers ?? {};
      (original.headers as Record<string, string>).Authorization = `Bearer ${data.accessToken}`;
      return api(original);
    } catch (e) {
      refreshQueue.forEach(({ reject }) => reject(e));
      refreshQueue = [];
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      // Notify AuthContext (or anyone else) that the session has expired.
      // AuthProvider / App can listen and redirect to /login.
      window.dispatchEvent(new Event('auth:expired'));
      return Promise.reject(e);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
