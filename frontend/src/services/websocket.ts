import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();

  connect(
    accessToken: string,
    onConnect?: () => void,
    onError?: (error: string) => void,
  ): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        onConnect?.();
      },
      onStompError: (frame) => {
        onError?.(frame.headers['message'] || 'WebSocket error');
      },
      onWebSocketClose: () => {
        console.log('WebSocket connection closed');
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    this.client?.deactivate();
    this.client = null;
  }

  subscribe(destination: string, callback: (message: IMessage) => void): void {
    if (!this.client?.connected) return;
    if (this.subscriptions.has(destination)) return;

    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);
  }

  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  publish(destination: string, body: object): void {
    if (!this.client?.connected) return;
    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

export const wsService = new WebSocketService();
