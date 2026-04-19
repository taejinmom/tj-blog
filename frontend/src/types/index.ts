export interface User {
  id: number;
  username: string;
  nickname: string;
  status: 'ONLINE' | 'OFFLINE' | 'AWAY';
  createdAt: string;
}

export interface ChatRoom {
  id: number;
  name: string;
  type: 'GROUP' | 'DIRECT';
  memberCount: number;
  createdAt: string;
  lastMessage?: string;
  unreadCount?: number;
}

export interface ChatMessage {
  id: number;
  roomId: number;
  senderId: number;
  senderNickname: string;
  content: string;
  type: 'CHAT' | 'JOIN' | 'LEAVE' | 'SYSTEM';
  createdAt: string;
  unreadCount: number;
}

export interface ReadUpdate {
  messageId: number;
  unreadCount: number;
}

export interface ReadReceipt {
  messageId: number;
  userId: number;
  readAt: string;
}

export interface Notification {
  id: string;
  type: 'MESSAGE' | 'JOIN' | 'LEAVE';
  roomId: number;
  roomName: string;
  senderNickname: string;
  content: string;
  createdAt: string;
}
