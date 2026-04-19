import { useEffect, useRef, useCallback, useState } from 'react';
import { wsService } from '../services/websocket';
import type { ChatMessage, ChatNotification } from '../types';

export function useWebSocket(
  userId: number | null,
  accessToken: string | null,
) {
  const [connected, setConnected] = useState(false);
  const [notifications, setNotifications] = useState<ChatNotification[]>([]);
  const connectedRef = useRef(false);

  useEffect(() => {
    if (!userId || !accessToken || connectedRef.current) return;

    wsService.connect(
      accessToken,
      () => {
        setConnected(true);
        connectedRef.current = true;

        wsService.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
          const notification: ChatNotification = JSON.parse(msg.body);
          setNotifications((prev) => [...prev, notification]);
          setTimeout(() => {
            setNotifications((prev) =>
              prev.filter((n) => n.id !== notification.id),
            );
          }, 5000);
        });
      },
      (error) => {
        console.error('WebSocket error:', error);
        setConnected(false);
        connectedRef.current = false;
      },
    );

    return () => {
      wsService.disconnect();
      setConnected(false);
      connectedRef.current = false;
    };
  }, [userId, accessToken]);

  const subscribeToRoom = useCallback(
    (roomId: number, onMessage: (message: ChatMessage) => void) => {
      const dest = `/topic/chat-room/${roomId}`;
      wsService.subscribe(dest, (msg) => {
        const chatMessage: ChatMessage = JSON.parse(msg.body);
        onMessage(chatMessage);
      });
      return () => wsService.unsubscribe(dest);
    },
    [],
  );

  const sendMessage = useCallback(
    (roomId: number, content: string, senderId: number) => {
      wsService.publish('/app/chat.send', {
        roomId,
        senderId,
        content,
        type: 'CHAT',
      });
    },
    [],
  );

  const sendTyping = useCallback((roomId: number, senderId: number) => {
    wsService.publish('/app/chat.typing', { roomId, senderId });
  }, []);

  const dismissNotification = useCallback((id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  }, []);

  return {
    connected,
    notifications,
    subscribeToRoom,
    sendMessage,
    sendTyping,
    dismissNotification,
  };
}
