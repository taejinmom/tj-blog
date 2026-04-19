import type { ChatMessage } from '../types';

interface Props {
  message: ChatMessage;
  isOwn: boolean;
}

function formatTime(dateStr: string): string {
  return new Date(dateStr).toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function MessageItem({ message, isOwn }: Props) {
  if (message.type === 'JOIN' || message.type === 'LEAVE' || message.type === 'SYSTEM') {
    return (
      <div className="flex justify-center my-2">
        <span className="text-xs text-dark-muted bg-dark-card px-3 py-1 rounded-full">
          {message.content}
        </span>
      </div>
    );
  }

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-1`}>
      <div className={`flex ${isOwn ? 'flex-row-reverse' : 'flex-row'} items-end gap-2 max-w-[75%]`}>
        {/* Avatar */}
        {!isOwn && (
          <div className="w-8 h-8 bg-dark-accent/30 rounded-full flex items-center justify-center text-xs font-medium text-dark-accent flex-shrink-0 mb-1">
            {message.senderNickname.charAt(0)}
          </div>
        )}

        <div className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'}`}>
          {/* Sender name */}
          {!isOwn && (
            <span className="text-xs text-dark-muted mb-1 ml-1">{message.senderNickname}</span>
          )}

          <div className={`flex ${isOwn ? 'flex-row-reverse' : 'flex-row'} items-end gap-1.5`}>
            {/* Message bubble */}
            <div
              className={`px-3 py-2 rounded-2xl text-sm break-words whitespace-pre-wrap ${
                isOwn
                  ? 'bg-dark-sent text-white rounded-br-md'
                  : 'bg-dark-received text-dark-text rounded-bl-md'
              }`}
            >
              {message.content}
            </div>

            {/* Time & unread count */}
            <div className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'} flex-shrink-0`}>
              {message.unreadCount > 0 && (
                <span className="text-[10px] text-yellow-400 font-medium">{message.unreadCount}</span>
              )}
              <span className="text-[10px] text-dark-muted">{formatTime(message.createdAt)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
