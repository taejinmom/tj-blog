package com.taejin.chat.service;

import com.taejin.chat.entity.User;
import com.taejin.chat.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * auth-service 에서 발급된 JWT 의 userId 가 chat-service 의 users 테이블에 아직
 * 없으면 즉석에서 stub row 를 만들어 준다 (자동 프로비저닝).
 *
 * 임시 방편이며, 정식 해결은 Phase 4 의 이벤트 기반 동기화 + chat.users 축소 로
 * 대체될 예정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void provisionIfMissing(Long userId, String email) {
        if (userId == null) return;
        if (userRepository.existsById(userId)) return;

        String username = email != null ? email : ("user-" + userId);
        String nickname = deriveNickname(email, userId);

        // id 를 JWT 의 sub 값(=authdb 의 user id) 그대로 사용해야 FK 가 맞는다.
        // JPA IDENTITY 전략은 id 수동 지정을 막으므로 native SQL 로 insert.
        em.createNativeQuery(
                "INSERT INTO users (id, username, nickname, status, created_at) " +
                "VALUES (?, ?, ?, 'OFFLINE', NOW()) ON CONFLICT (id) DO NOTHING")
                .setParameter(1, userId)
                .setParameter(2, username)
                .setParameter(3, nickname)
                .executeUpdate();

        log.info("Provisioned chat user stub: id={} nickname={}", userId, nickname);
    }

    private String deriveNickname(String email, Long userId) {
        if (email == null || email.isBlank()) return "user-" + userId;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
