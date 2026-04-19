import { useEffect, useState } from 'react';
import { useParams, useOutletContext, useNavigate } from 'react-router-dom';
import { chatRoomApi } from '../services/api';
import { useChat } from '../hooks/useChat';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import type { ChatMessage, ChatRoom as ChatRoomType, User } from '../types';

interface OutletContext {
  subscribeToRoom: (roomId: number, onMessage: (msg: ChatMessage) => void) => () => void;
  sendMessage: (roomId: number, content: string, senderId: number) => void;
  userId: number;
  connected: boolean;
}

export default function ChatRoom() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const { subscribeToRoom, sendMessage: wsSend, userId, connected } = useOutletContext<OutletContext>();
  const [room, setRoom] = useState<ChatRoomType | null>(null);
  const [members, setMembers] = useState<User[]>([]);
  const [showMembers, setShowMembers] = useState(false);

  const numRoomId = roomId ? Number(roomId) : null;
  const { messages, loading, sendMessage } = useChat(numRoomId, userId, subscribeToRoom, wsSend, connected);

  useEffect(() => {
    if (!numRoomId) return;

    chatRoomApi.getRoom(numRoomId).then((res) => setRoom(res.data)).catch(() => {});

    // join 후 멤버 목록 갱신
    chatRoomApi.joinRoom(numRoomId, userId)
      .then(() => chatRoomApi.getMembers(numRoomId).then((res) => setMembers(res.data)))
      .catch(() => chatRoomApi.getMembers(numRoomId).then((res) => setMembers(res.data)).catch(() => {}));
  }, [numRoomId, userId]);

  if (!roomId) {
    return (
      <div className="flex-1 flex items-center justify-center text-dark-muted">
        <div className="text-center">
          <svg className="w-16 h-16 mx-auto mb-4 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <p className="text-lg">채팅방을 선택하세요</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* Room Header */}
      <div className="px-4 py-3 bg-dark-sidebar border-b border-dark-border flex items-center justify-between flex-shrink-0">
        <div className="min-w-0">
          <h2 className="font-bold truncate">{room?.name || '...'}</h2>
          <p className="text-xs text-dark-muted">
            {room?.memberCount || members.length}명 참여중
          </p>
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          <button
            onClick={() => {
              if (!numRoomId) return;
              if (confirm('채팅방을 나가시겠습니까?')) {
                chatRoomApi.leaveRoom(numRoomId, userId).then(() => navigate('/chat')).catch(() => {});
              }
            }}
            className="p-2 rounded-lg hover:bg-red-500/20 text-dark-muted hover:text-red-400 transition-colors"
            title="채팅방 나가기"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
          <button
            onClick={() => setShowMembers(!showMembers)}
            className="p-2 rounded-lg hover:bg-dark-hover text-dark-muted hover:text-dark-text transition-colors"
            title="멤버 목록"
          >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          </button>
        </div>
      </div>

      <div className="flex-1 flex min-h-0">
        {/* Messages */}
        <div className="flex-1 flex flex-col min-h-0">
          <MessageList messages={messages} userId={userId} loading={loading} />
          <MessageInput onSend={sendMessage} />
        </div>

        {/* Members Panel */}
        {showMembers && (
          <div className="w-56 bg-dark-sidebar border-l border-dark-border flex-shrink-0 overflow-y-auto">
            <div className="p-3 border-b border-dark-border">
              <h3 className="text-sm font-medium text-dark-muted">멤버 ({members.length})</h3>
            </div>
            {members.map((member) => (
              <div key={member.id} className="px-3 py-2 flex items-center gap-2 hover:bg-dark-hover">
                <div className="w-7 h-7 bg-dark-accent/30 rounded-full flex items-center justify-center text-xs font-medium text-dark-accent flex-shrink-0">
                  {member.nickname.charAt(0)}
                </div>
                <span className="text-sm truncate">{member.nickname}</span>
                {member.status === 'ONLINE' && (
                  <div className="w-2 h-2 bg-green-400 rounded-full flex-shrink-0 ml-auto" />
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
