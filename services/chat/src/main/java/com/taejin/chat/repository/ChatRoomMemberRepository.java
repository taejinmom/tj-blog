package com.taejin.chat.repository;

import com.taejin.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    int countByChatRoomId(Long chatRoomId);

    void deleteByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
