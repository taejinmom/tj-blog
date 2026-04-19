package com.taejin.chat.repository;

import com.taejin.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);

    java.util.List<ChatMessage> findByChatRoomIdAndIdLessThanEqualOrderByIdAsc(Long chatRoomId, Long id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ChatMessage m WHERE m.chatRoom.id = :roomId")
    void deleteByChatRoomId(Long roomId);
}
