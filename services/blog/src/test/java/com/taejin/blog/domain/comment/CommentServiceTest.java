package com.taejin.blog.domain.comment;

import com.taejin.blog.domain.post.Post;
import com.taejin.blog.domain.post.PostRepository;
import com.taejin.blog.exception.ForbiddenException;
import com.taejin.blog.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CommentService.class)
class CommentServiceTest {

    @Autowired CommentService commentService;
    @Autowired PostRepository postRepository;

    Long postId;

    @BeforeEach
    void setUp() {
        Post post = postRepository.save(new Post("글", "내용", "Tech", List.of()));
        postId = post.getId();
    }

    private CommentRequest req(String content) {
        CommentRequest r = new CommentRequest();
        r.setContent(content);
        return r;
    }

    @Test
    void 댓글_작성시_표시명은_이메일_로컬파트() {
        CommentResponse res = commentService.create(postId, 10L, "alice@example.com", req("안녕"));
        assertThat(res.authorName()).isEqualTo("alice");
        assertThat(res.content()).isEqualTo("안녕");
        assertThat(commentService.findByPost(postId)).hasSize(1);
    }

    @Test
    void 없는_글에_댓글_작성시_404() {
        assertThatThrownBy(() -> commentService.create(99999L, 10L, "a@b.com", req("x")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 타인_댓글_삭제는_금지() {
        CommentResponse c = commentService.create(postId, 10L, "alice@example.com", req("내 댓글"));
        assertThatThrownBy(() -> commentService.delete(c.id(), 11L, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 작성자_본인은_삭제_가능() {
        CommentResponse c = commentService.create(postId, 10L, "alice@example.com", req("내 댓글"));
        commentService.delete(c.id(), 10L, false);
        assertThat(commentService.findByPost(postId)).isEmpty();
    }

    @Test
    void 관리자는_타인_댓글_삭제_가능() {
        CommentResponse c = commentService.create(postId, 10L, "alice@example.com", req("내 댓글"));
        commentService.delete(c.id(), 999L, true);
        assertThat(commentService.findByPost(postId)).isEmpty();
    }
}
