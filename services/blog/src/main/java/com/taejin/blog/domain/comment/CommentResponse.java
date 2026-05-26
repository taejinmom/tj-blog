package com.taejin.blog.domain.comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(), c.getPostId(), c.getAuthorId(),
                c.getAuthorName(), c.getContent(), c.getCreatedAt());
    }
}
