import { useState, useEffect, useCallback, useRef } from 'react';
import { messageApi } from '../services/api';
import { wsService } from '../services/websocket';
import type { ChatMessage, ReadUpdate } from '../types';

export function useChat(
  roomId: number | null,
  userId: number | null,
  subscribeToRoom: (roomId: number, onMessage: (msg: ChatMessage) => void) => () => void,
  wsSendMessage: (roomId: number, content: string, senderId: number) => void,
  connected: boolean
) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const unsubRef = useRef<(() => void) | null>(null);
  const readUnsubRef = useRef<(() => void) | null>(null);

  const markLastAsRead = (msgs: ChatMessage[]) => {
    if (!userId || msgs.length === 0) return;
    const lastMsg = msgs[msgs.length - 1];
    if (lastMsg.senderId !== userId) {
      messageApi.markAsRead(lastMsg.id, userId).catch(() => {});
    }
  };

  useEffect(() => {
    if (!roomId || !userId || !connected) return;

    setMessages([]);
    setLoading(true);

    messageApi.getMessages(roomId).then((res) => {
      setMessages(res.data);
      setLoading(false);
      markLastAsRead(res.data);
    }).catch(() => setLoading(false));

    // 메시지 구독
    unsubRef.current?.();
    unsubRef.current = subscribeToRoom(roomId, (msg) => {
      setMessages((prev) => {
        const updated = [...prev, msg];
        if (msg.senderId !== userId) {
          messageApi.markAsRead(msg.id, userId!).catch(() => {});
        }
        return updated;
      });
    });

    // 읽음 업데이트 구독
    readUnsubRef.current?.();
    const readDest = `/topic/chat-room/${roomId}/read`;
    wsService.subscribe(readDest, (msg) => {
      const update: ReadUpdate = JSON.parse(msg.body);
      setMessages((prev) =>
        prev.map((m) =>
          m.id === update.messageId ? { ...m, unreadCount: update.unreadCount } : m
        )
      );
    });
    readUnsubRef.current = () => wsService.unsubscribe(readDest);

    return () => {
      unsubRef.current?.();
      unsubRef.current = null;
      readUnsubRef.current?.();
      readUnsubRef.current = null;
    };
  }, [roomId, userId, subscribeToRoom, connected]);

  const sendMessage = useCallback(
    (content: string) => {
      if (!roomId || !userId || !content.trim()) return;
      wsSendMessage(roomId, content.trim(), userId);
    },
    [roomId, userId, wsSendMessage]
  );

  return { messages, loading, sendMessage };
}
