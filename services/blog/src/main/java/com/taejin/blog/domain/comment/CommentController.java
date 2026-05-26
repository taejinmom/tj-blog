package com.taejin.blog.domain.comment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 게시물의 댓글 목록 (공개). */
    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> list(@PathVariable Long postId) {
        return commentService.findByPost(postId);
    }

    /** 댓글 작성 (로그인 필요). 작성자는 토큰에서 추출. */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> create(@PathVariable Long postId,
                                                  @Valid @RequestBody CommentRequest request,
                                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String email = (String) auth.getCredentials();
        CommentResponse created = commentService.create(postId, userId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 댓글 삭제 (작성자 본인 또는 관리자). */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        commentService.delete(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
