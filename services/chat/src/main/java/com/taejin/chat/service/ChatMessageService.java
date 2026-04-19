package com.taejin.chat.service;

import com.taejin.chat.dto.*;
import com.taejin.chat.entity.*;
import com.taejin.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ReadReceiptRepository readReceiptRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse save(ChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .type(ChatMessage.MessageType.valueOf(request.getType()))
                .build();

        ChatMessage saved = messageRepository.save(message);

        // 발신자는 자동으로 읽음 처리
        ReadReceipt senderReceipt = ReadReceipt.builder()
                .message(saved)
                .user(sender)
                .build();
        readReceiptRepository.save(senderReceipt);

        // unreadCount = 전체 멤버 수 - 읽은 수(발신자 1명)
        int totalMembers = memberRepository.countByChatRoomId(room.getId());
        int readCount = 1; // 발신자
        int unreadCount = Math.max(0, totalMembers - readCount);

        return ChatMessageResponse.from(saved, unreadCount);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, int page, int size) {
        int totalMembers = memberRepository.countByChatRoomId(roomId);

        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId, PageRequest.of(page, size))
                .map(msg -> {
                    int readCount = readReceiptRepository.countByMessageId(msg.getId());
                    int unreadCount = Math.max(0, totalMembers - readCount);
                    return ChatMessageResponse.from(msg, unreadCount);
                })
                .getContent();
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        if (readReceiptRepository.existsByMessageIdAndUserId(messageId, userId)) {
            return;
        }
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ReadReceipt receipt = ReadReceipt.builder()
                .message(message)
                .user(user)
                .build();
        readReceiptRepository.save(receipt);

        // 읽음 처리 후 업데이트된 unreadCount를 해당 채팅방에 브로드캐스트
        int totalMembers = memberRepository.countByChatRoomId(message.getChatRoom().getId());
        int readCount = readReceiptRepository.countByMessageId(messageId);
        int unreadCount = Math.max(0, totalMembers - readCount);

        messagingTemplate.convertAndSend(
                "/topic/chat-room/" + message.getChatRoom().getId() + "/read",
                new ReadUpdate(messageId, unreadCount)
        );
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ReadUpdate {
        private Long messageId;
        private int unreadCount;
    }
}
