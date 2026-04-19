import { useNavigate } from 'react-router-dom';
import type { Notification } from '../types';

interface Props {
  notifications: Notification[];
  onDismiss: (id: string) => void;
}

export default function NotificationToast({ notifications, onDismiss }: Props) {
  const navigate = useNavigate();

  if (notifications.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-50 space-y-2 max-w-sm">
      {notifications.map((notif) => (
        <div
          key={notif.id}
          className="bg-dark-card border border-dark-border rounded-xl p-4 shadow-xl
            animate-slide-in cursor-pointer hover:bg-dark-hover transition-colors"
          onClick={() => {
            navigate(`/chat/${notif.roomId}`);
            onDismiss(notif.id);
          }}
        >
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <p className="text-sm font-medium truncate">{notif.roomName}</p>
              <p className="text-xs text-dark-muted mt-0.5">
                <span className="font-medium text-dark-text">{notif.senderNickname}</span>
                {': '}
                <span className="truncate">{notif.content}</span>
              </p>
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDismiss(notif.id);
              }}
              className="text-dark-muted hover:text-dark-text flex-shrink-0"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
