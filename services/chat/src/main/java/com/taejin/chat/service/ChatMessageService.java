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

    /**
     * 시스템 메시지(JOIN/LEAVE 등)를 저장하고 채팅방에 즉시 브로드캐스트한다.
     * 발신자(해당 유저)는 자동 읽음 처리.
     */
    @Transactional
    public ChatMessageResponse saveSystemMessage(Long roomId, Long userId, String content,
                                                  ChatMessage.MessageType type) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .chatRoom(room).sender(user).content(content).type(type).build());
        readReceiptRepository.save(ReadReceipt.builder().message(saved).user(user).build());

        int totalMembers = memberRepository.countByChatRoomId(roomId);
        int unreadCount = Math.max(0, totalMembers - 1);
        ChatMessageResponse response = ChatMessageResponse.from(saved, unreadCount);

        messagingTemplate.convertAndSend("/topic/chat-room/" + roomId, response);
        return response;
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

    /**
     * messageId 이하의 모든 메시지를 userId의 읽음으로 처리하고, 실제로 새로 읽음
     * 처리된 메시지에 대해서만 /topic/chat-room/{roomId}/read 로 ReadUpdate 브로드캐스트.
     * "message N 읽음" 의미가 "1..N 까지 전부 읽음" 이 되도록 cascade.
     */
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        ChatMessage target = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Long roomId = target.getChatRoom().getId();

        List<ChatMessage> candidates = messageRepository
                .findByChatRoomIdAndIdLessThanEqualOrderByIdAsc(roomId, messageId);
        int totalMembers = memberRepository.countByChatRoomId(roomId);

        for (ChatMessage msg : candidates) {
            if (readReceiptRepository.existsByMessageIdAndUserId(msg.getId(), userId)) {
                continue;
            }
            readReceiptRepository.save(ReadReceipt.builder().message(msg).user(user).build());

            int readCount = readReceiptRepository.countByMessageId(msg.getId());
            int unreadCount = Math.max(0, totalMembers - readCount);
            messagingTemplate.convertAndSend(
                    "/topic/chat-room/" + roomId + "/read",
                    new ReadUpdate(msg.getId(), unreadCount)
            );
        }
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ReadUpdate {
        private Long messageId;
        private int unreadCount;
    }
}
