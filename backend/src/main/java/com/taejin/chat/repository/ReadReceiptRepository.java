package com.taejin.chat.repository;

import com.taejin.chat.entity.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, Long> {

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    int countByMessageId(Long messageId);
}
