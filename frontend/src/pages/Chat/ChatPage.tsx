import { useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useWebSocket } from '../../hooks/useWebSocket';
import ChatRoomList from './ChatRoomList';
import ChatRoom from './ChatRoom';
import NotificationToast from './NotificationToast';

export default function ChatPage() {
  const { user, accessToken } = useAuth();
  const userId = user?.id ?? null;

  const { connected, notifications, subscribeToRoom, sendMessage, dismissNotification } =
    useWebSocket(userId, accessToken);

  const { roomId } = useParams();

  return (
    <div className="h-[calc(100vh-4rem)] flex bg-dark-bg text-dark-text">
      {/* Sidebar */}
      <div className="w-80 flex-shrink-0 bg-dark-sidebar border-r border-dark-border flex flex-col overflow-hidden">
        <div className="p-4 border-b border-dark-border flex items-center justify-between">
          <div className="flex items-center gap-2 min-w-0">
            <div
              className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${
                connected ? 'bg-green-400' : 'bg-red-400'
              }`}
            />
            <span className="font-medium truncate">
              {user?.displayName || user?.email || '게스트'}
            </span>
          </div>
        </div>

        <div className="flex-1 overflow-hidden">
          <ChatRoomList connected={connected} />
        </div>
      </div>

      {/* Main pane */}
      <div className="flex-1 flex flex-col min-w-0">
        {roomId && userId !== null ? (
          <ChatRoom
            subscribeToRoom={subscribeToRoom}
            sendMessage={sendMessage}
            userId={userId}
            connected={connected}
          />
        ) : (
          <div className="flex-1 flex items-center justify-center text-dark-muted">
            <div className="text-center">
              <svg
                className="w-16 h-16 mx-auto mb-4 opacity-30"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                />
              </svg>
              <p className="text-lg">
                {userId === null ? '로그인이 필요합니다' : '채팅방을 선택하세요'}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Notifications */}
      <NotificationToast notifications={notifications} onDismiss={dismissNotification} />
    </div>
  );
}
