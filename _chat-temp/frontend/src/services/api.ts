import axios from 'axios';
import type { User, ChatRoom, ChatMessage } from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const userId = localStorage.getItem('userId');
  if (userId) {
    config.headers['X-User-Id'] = userId;
  }
  return config;
});

export const userApi = {
  register: (data: { username: string; nickname: string; password: string }) =>
    api.post<User>('/users', data),

  login: (data: { username: string; password: string }) =>
    api.post<User>('/users/login', data),

  getUsers: () => api.get<User[]>('/users'),
};

export const chatRoomApi = {
  getRooms: () => api.get<ChatRoom[]>('/chat-rooms'),

  createRoom: (data: { name: string; type?: string; creatorId: number }) =>
    api.post<ChatRoom>('/chat-rooms', data),

  getRoom: (roomId: number) => api.get<ChatRoom>(`/chat-rooms/${roomId}`),

  joinRoom: (roomId: number, userId: number) =>
    api.post(`/chat-rooms/${roomId}/join`, { userId }),

  getMembers: (roomId: number) => api.get<User[]>(`/chat-rooms/${roomId}/members`),

  leaveRoom: (roomId: number, userId: number) =>
    api.post(`/chat-rooms/${roomId}/leave`, { userId }),
};

export const messageApi = {
  getMessages: (roomId: number, page = 0, size = 50) =>
    api.get<ChatMessage[]>(`/chat-rooms/${roomId}/messages`, { params: { page, size } }),

  markAsRead: (messageId: number, userId: number) =>
    api.post(`/messages/${messageId}/read`, { userId }),
};

export default api;
