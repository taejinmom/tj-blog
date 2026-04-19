import { useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import ChatRoomList from './ChatRoomList';
import UserList from './UserList';
import NotificationToast from './NotificationToast';
import { useWebSocket } from '../hooks/useWebSocket';

export default function Layout() {
  const navigate = useNavigate();
  const [showUsers, setShowUsers] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const userId = Number(localStorage.getItem('userId'));
  const nickname = localStorage.getItem('nickname') || '';

  const { connected, notifications, subscribeToRoom, sendMessage, dismissNotification } =
    useWebSocket(userId || null);

  useEffect(() => {
    if (!userId) {
      navigate('/');
      return;
    }
    // DB 초기화 등으로 userId가 유효하지 않을 경우 로그인 페이지로 이동
    import('../services/api').then(({ userApi }) => {
      userApi.getUsers().then((res) => {
        const exists = res.data.some((u) => u.id === userId);
        if (!exists) {
          localStorage.clear();
          navigate('/');
        }
      }).catch(() => {
        localStorage.clear();
        navigate('/');
      });
    });
  }, [userId, navigate]);

  const handleLogout = () => {
    localStorage.clear();
    navigate('/');
  };

  return (
    <div className="h-screen flex bg-dark-bg">
      {/* Sidebar */}
      <div
        className={`${
          sidebarOpen ? 'w-80' : 'w-0'
        } flex-shrink-0 bg-dark-sidebar border-r border-dark-border flex flex-col transition-all duration-300 overflow-hidden
        max-md:absolute max-md:z-20 max-md:h-full ${sidebarOpen ? 'max-md:w-72' : ''}`}
      >
        {/* Header */}
        <div className="p-4 border-b border-dark-border flex items-center justify-between">
          <div className="flex items-center gap-2 min-w-0">
            <div
              className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${
                connected ? 'bg-green-400' : 'bg-red-400'
              }`}
            />
            <span className="font-medium truncate">{nickname}</span>
          </div>
          <div className="flex items-center gap-1 flex-shrink-0">
            <button
              onClick={() => setShowUsers(!showUsers)}
              className="p-2 rounded-lg hover:bg-dark-hover text-dark-muted hover:text-dark-text transition-colors"
              title="접속자 목록"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
              </svg>
            </button>
            <button
              onClick={handleLogout}
              className="p-2 rounded-lg hover:bg-dark-hover text-dark-muted hover:text-dark-text transition-colors"
              title="로그아웃"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-hidden">
          {showUsers ? <UserList /> : <ChatRoomList />}
        </div>
      </div>

      {/* Mobile sidebar toggle */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        className="md:hidden fixed bottom-4 left-4 z-30 p-3 bg-dark-accent text-white rounded-full shadow-lg"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d={sidebarOpen ? 'M6 18L18 6M6 6l12 12' : 'M4 6h16M4 12h16M4 18h16'} />
        </svg>
      </button>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        <Outlet context={{ subscribeToRoom, sendMessage, userId, connected }} />
      </div>

      {/* Notifications */}
      <NotificationToast notifications={notifications} onDismiss={dismissNotification} />
    </div>
  );
}
