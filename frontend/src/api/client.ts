import axios from 'axios';
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
