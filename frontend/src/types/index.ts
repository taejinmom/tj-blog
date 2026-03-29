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
