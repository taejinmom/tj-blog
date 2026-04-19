import { useEffect, useRef } from 'react';
import MessageItem from './MessageItem';
import type { ChatMessage } from '../types';

interface Props {
  messages: ChatMessage[];
  userId: number;
  loading: boolean;
}

function formatDateHeader(dateStr: string): string {
  const date = new Date(dateStr);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  if (date.toDateString() === today.toDateString()) return '오늘';
  if (date.toDateString() === yesterday.toDateString()) return '어제';

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  });
}

export default function MessageList({ messages, userId, loading }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-2 border-dark-accent border-t-transparent rounded-full" />
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center text-dark-muted text-sm">
        아직 메시지가 없습니다. 첫 메시지를 보내보세요!
      </div>
    );
  }

  let lastDate = '';

  return (
    <div ref={containerRef} className="flex-1 overflow-y-auto px-4 py-3 space-y-1">
      {messages.map((msg) => {
        const msgDate = new Date(msg.createdAt).toDateString();
        let showDateHeader = false;
        if (msgDate !== lastDate) {
          showDateHeader = true;
          lastDate = msgDate;
        }

        return (
          <div key={msg.id}>
            {showDateHeader && (
              <div className="flex items-center justify-center my-4">
                <div className="bg-dark-card px-3 py-1 rounded-full text-xs text-dark-muted">
                  {formatDateHeader(msg.createdAt)}
                </div>
              </div>
            )}
            <MessageItem message={msg} isOwn={msg.senderId === userId} />
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
