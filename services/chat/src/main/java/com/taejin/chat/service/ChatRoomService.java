package com.taejin.chat.service;

import com.taejin.chat.dto.*;
import com.taejin.chat.entity.*;
import com.taejin.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadReceiptRepository readReceiptRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> findAll() {
        return chatRoomRepository.findAll().stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional
    public ChatRoomResponse create(ChatRoomRequest request) {
        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(ChatRoom.RoomType.valueOf(request.getType()))
                .build();
        room = chatRoomRepository.save(room);

        if (request.getCreatorId() != null) {
            User creator = userRepository.findById(request.getCreatorId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            ChatRoomMember member = ChatRoomMember.builder()
                    .chatRoom(room)
                    .user(creator)
                    .build();
            memberRepository.save(member);
            room.getMembers().add(member);
        }

        broadcastRoomsChanged();
        return ChatRoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse findById(Long id) {
        return ChatRoomResponse.from(chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + id)));
    }

    @Transactional
    public void join(Long roomId, Long userId) {
        if (memberRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            return;
        }
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(room)
                .user(user)
                .build();
        memberRepository.save(member);

        // 시스템 메시지 + 실시간 브로드캐스트
        chatMessageService.saveSystemMessage(
                roomId, userId,
                user.getNickname() + "님이 입장했습니다",
                ChatMessage.MessageType.JOIN
        );
        broadcastMembersChanged(roomId);
        broadcastRoomsChanged();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getMembers(Long roomId) {
        return memberRepository.findByChatRoomId(roomId).stream()
                .map(m -> UserResponse.from(m.getUser()))
                .toList();
    }

    @Transactional
    public void leave(Long roomId, Long userId) {
        if (!memberRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 멤버 제거 전에 LEAVE 메시지 브로드캐스트 — 본인도 마지막 메시지로 볼 수 있게.
        chatMessageService.saveSystemMessage(
                roomId, userId,
                user.getNickname() + "님이 퇴장했습니다",
                ChatMessage.MessageType.LEAVE
        );

        memberRepository.deleteByChatRoomIdAndUserId(roomId, userId);

        // 멤버가 0명이면 채팅방과 그에 속한 메시지/읽음기록까지 정리
        // (FK에 ON DELETE CASCADE 가 걸려있지 않아 명시적으로 삭제)
        if (memberRepository.countByChatRoomId(roomId) == 0) {
            readReceiptRepository.deleteByChatRoomId(roomId);
            chatMessageRepository.deleteByChatRoomId(roomId);
            chatRoomRepository.deleteById(roomId);
        } else {
            broadcastMembersChanged(roomId);
        }
        broadcastRoomsChanged();
    }

    private void broadcastMembersChanged(Long roomId) {
        List<UserResponse> members = memberRepository.findByChatRoomId(roomId).stream()
                .map(m -> UserResponse.from(m.getUser()))
                .toList();
        messagingTemplate.convertAndSend(
                "/topic/chat-room/" + roomId + "/members",
                Map.of("memberCount", members.size(), "members", members)
        );
    }

    private void broadcastRoomsChanged() {
        messagingTemplate.convertAndSend("/topic/chat-rooms", Map.of("event", "updated"));
    }
}
