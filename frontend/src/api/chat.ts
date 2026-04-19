import client from './client';
import type { ChatRoom, ChatMessage, ChatMember } from '../types';

// NOTE: chat backend (services/chat) still expects userId/creatorId in request
// bodies for join/leave/create/markAsRead. Phase 2 has not rewired those
// controllers to read the user id from the JWT yet, so we pass them explicitly
// from the caller (user.id from useAuth()). Once the backend switches to
// deriving userId from the JWT, drop the userId arguments below.

export const chatRoomApi = {
  getRooms: () => client.get<ChatRoom[]>('/chat-rooms').then((r) => r.data),

  createRoom: (data: { name: string; type?: string; creatorId: number }) =>
    client.post<ChatRoom>('/chat-rooms', data).then((r) => r.data),

  getRoom: (id: number) =>
    client.get<ChatRoom>(`/chat-rooms/${id}`).then((r) => r.data),

  joinRoom: (id: number, userId: number) =>
    client.post(`/chat-rooms/${id}/join`, { userId }),

  leaveRoom: (id: number, userId: number) =>
    client.post(`/chat-rooms/${id}/leave`, { userId }),

  getMembers: (id: number) =>
    client.get<ChatMember[]>(`/chat-rooms/${id}/members`).then((r) => r.data),
};

export const messageApi = {
  getMessages: (roomId: number, page = 0, size = 50) =>
    client
      .get<ChatMessage[]>(`/chat-rooms/${roomId}/messages`, {
        params: { page, size },
      })
      .then((r) => r.data),

  markAsRead: (messageId: number, userId: number) =>
    client.post(`/messages/${messageId}/read`, { userId }),
};
