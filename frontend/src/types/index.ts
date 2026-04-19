// ===== Blog =====
export interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  createdAt: string;
  updatedAt: string;
}

export interface PostRequest {
  title: string;
  content: string;
  category: string;
}

// ===== Todo =====
export type TodoStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export interface TodoItem {
  id: number;
  title: string;
  description: string;
  phase: string;
  status: TodoStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TodoRequest {
  title: string;
  description: string;
  phase: string;
  status: TodoStatus;
}

// ===== Auth =====
export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  roles: string[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Claims produced by auth-service JwtTokenProvider. */
export interface JwtAccessClaims {
  sub: string;           // user id as string
  email: string;
  roles: string[];
  typ: 'access';
  iss: string;
  iat: number;
  exp: number;
}

// ===== Chat =====
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

export interface ChatMember {
  id: number;
  username: string;
  nickname: string;
  status: 'ONLINE' | 'OFFLINE' | 'AWAY';
  createdAt: string;
}

export interface ReadUpdate {
  messageId: number;
  unreadCount: number;
}

export interface ChatNotification {
  id: string;
  type: 'MESSAGE' | 'JOIN' | 'LEAVE';
  roomId: number;
  roomName: string;
  senderNickname: string;
  content: string;
  createdAt: string;
}
