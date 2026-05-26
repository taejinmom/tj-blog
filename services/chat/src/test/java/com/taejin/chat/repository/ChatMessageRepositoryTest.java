package com.taejin.chat.repository;

import com.taejin.chat.entity.ChatMessage;
import com.taejin.chat.entity.ChatRoom;
import com.taejin.chat.entity.ReadReceipt;
import com.taejin.chat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatMessageRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ChatMessageRepository messageRepository;

    Long roomId;
    Long aliceId;
    Long bobId;

    @BeforeEach
    void setUp() {
        User alice = em.persist(User.builder().username("alice@e.com").nickname("alice").build());
        User bob = em.persist(User.builder().username("bob@e.com").nickname("bob").build());
        ChatRoom room = em.persist(ChatRoom.builder().name("room").type(ChatRoom.RoomType.GROUP).build());
        aliceId = alice.getId();
        bobId = bob.getId();
        roomId = room.getId();

        java.time.LocalDateTime t0 = java.time.LocalDateTime.now();

        // bob 이 보낸 메시지 (bob 은 자동 읽음)
        ChatMessage m1 = em.persist(ChatMessage.builder()
                .chatRoom(room).sender(bob).content("안녕하세요").type(ChatMessage.MessageType.TEXT)
                .createdAt(t0).build());
        em.persist(ReadReceipt.builder().message(m1).user(bob).build());

        // bob 이 보낸 두 번째 메시지 (더 나중)
        em.persist(ChatMessage.builder()
                .chatRoom(room).sender(bob).content("마지막 메시지").type(ChatMessage.MessageType.TEXT)
                .createdAt(t0.plusSeconds(1)).build());
        em.flush();
    }

    @Test
    void 안읽음수는_사용자별로_계산된다() {
        // alice 는 bob 의 메시지 2개 모두 안읽음
        assertThat(messageRepository.countUnread(roomId, aliceId)).isEqualTo(2);
        // bob 은 발신자이므로 자신의 메시지는 안읽음에서 제외 → 0
        assertThat(messageRepository.countUnread(roomId, bobId)).isZero();
    }

    @Test
    void 최근_메시지를_가져온다() {
        ChatMessage last = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(roomId);
        assertThat(last).isNotNull();
        assertThat(last.getContent()).isEqualTo("마지막 메시지");
    }
}
