package com.taejin.blog.domain.comment;

import com.taejin.blog.exception.ForbiddenException;
import com.taejin.blog.exception.NotFoundException;
import com.taejin.blog.domain.post.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public List<CommentResponse> findByPost(Long postId) {
        requirePostExists(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse create(Long postId, Long authorId, String email, CommentRequest request) {
        requirePostExists(postId);
        Comment saved = commentRepository.save(
                new Comment(postId, authorId, displayName(email), request.getContent()));
        return CommentResponse.from(saved);
    }

    @Transactional
    public void delete(Long commentId, Long currentUserId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
        if (!isAdmin && !comment.getAuthorId().equals(currentUserId)) {
            throw new ForbiddenException("본인 댓글만 삭제할 수 있습니다");
        }
        commentRepository.delete(comment);
    }

    private void requirePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("Post not found: " + postId);
        }
    }

    /** 이메일 전체 노출을 피해 로컬파트만 표시명으로 사용. */
    private static String displayName(String email) {
        if (email == null || email.isBlank()) return "익명";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
