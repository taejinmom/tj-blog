package com.taejin.chat.service;

import com.taejin.chat.dto.*;
import com.taejin.chat.entity.*;
import com.taejin.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;

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
        memberRepository.deleteByChatRoomIdAndUserId(roomId, userId);

        // 멤버가 0명이면 채팅방 삭제
        if (memberRepository.countByChatRoomId(roomId) == 0) {
            chatRoomRepository.deleteById(roomId);
        }
    }
}
