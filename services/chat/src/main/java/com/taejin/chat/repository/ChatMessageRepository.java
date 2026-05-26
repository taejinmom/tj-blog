package com.taejin.chat.repository;

import com.taejin.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndIdLessThanEqualOrderByIdAsc(Long chatRoomId, Long id);

    /** 방의 가장 최근 메시지 (없으면 null). */
    ChatMessage findFirstByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    /**
     * 특정 사용자 기준 안읽은 메시지 수.
     * 본인이 보낸 메시지는 제외하고, 해당 사용자의 읽음기록(ReadReceipt)이 없는 메시지를 센다.
     */
    @Query("""
            select count(m) from ChatMessage m
            where m.chatRoom.id = :roomId
              and m.sender.id <> :userId
              and not exists (
                  select 1 from ReadReceipt r where r.message = m and r.user.id = :userId
              )
            """)
    long countUnread(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.chatRoom.id = :roomId")
    void deleteByChatRoomId(Long roomId);
}
