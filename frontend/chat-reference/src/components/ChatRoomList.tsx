import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { chatRoomApi } from '../services/api';
import CreateRoomModal from './CreateRoomModal';
import type { ChatRoom } from '../types';

export default function ChatRoomList() {
  const navigate = useNavigate();
  const { roomId } = useParams();
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchRooms = () => {
    chatRoomApi.getRooms().then((res) => {
      setRooms(res.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchRooms();
    const interval = setInterval(fetchRooms, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleRoomCreated = () => {
    setShowModal(false);
    fetchRooms();
  };

  return (
    <div className="flex flex-col h-full">
      <div className="p-3 border-b border-dark-border">
        <button
          onClick={() => setShowModal(true)}
          className="w-full py-2 px-3 bg-dark-accent text-white rounded-lg text-sm font-medium
            hover:bg-blue-600 transition-colors flex items-center justify-center gap-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          새 채팅방
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin w-6 h-6 border-2 border-dark-accent border-t-transparent rounded-full" />
          </div>
        ) : rooms.length === 0 ? (
          <div className="text-center py-8 text-dark-muted text-sm">
            채팅방이 없습니다
          </div>
        ) : (
          rooms.map((room) => (
            <button
              key={room.id}
              onClick={() => navigate(`/chat/${room.id}`)}
              className={`w-full px-4 py-3 text-left hover:bg-dark-hover transition-colors border-b border-dark-border/50
                ${String(room.id) === roomId ? 'bg-dark-hover' : ''}`}
            >
              <div className="flex items-center justify-between">
                <span className="font-medium text-sm truncate">{room.name}</span>
                <span className="text-xs text-dark-muted flex-shrink-0 ml-2">
                  {room.memberCount}명
                </span>
              </div>
              <div className="flex items-center justify-between mt-1">
                <span className="text-xs text-dark-muted truncate">
                  {room.type === 'GROUP' ? '그룹 채팅' : '1:1 채팅'}
                </span>
                {room.unreadCount && room.unreadCount > 0 ? (
                  <span className="bg-dark-accent text-white text-xs rounded-full px-1.5 py-0.5 min-w-[20px] text-center flex-shrink-0">
                    {room.unreadCount}
                  </span>
                ) : null}
              </div>
            </button>
          ))
        )}
      </div>

      {showModal && (
        <CreateRoomModal onClose={() => setShowModal(false)} onCreated={handleRoomCreated} />
      )}
    </div>
  );
}
