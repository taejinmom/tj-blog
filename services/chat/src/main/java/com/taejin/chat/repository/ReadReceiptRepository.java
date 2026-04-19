package com.taejin.chat.repository;

import com.taejin.chat.entity.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, Long> {

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    int countByMessageId(Long messageId);

    @Modifying
    @Query("DELETE FROM ReadReceipt r WHERE r.message.chatRoom.id = :roomId")
    void deleteByChatRoomId(Long roomId);
}
