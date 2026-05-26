package com.taejin.blog.domain.post;

import com.taejin.blog.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 게시물 목록. 페이지네이션 + 검색(q) + 카테고리/태그 필터 지원.
     * 예: /api/posts?page=0&size=6&q=spring&category=Tech&tag=jpa
     */
    @GetMapping
    public PageResponse<Post> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 6, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(postService.list(q, category, tag, pageable));
    }

    @GetMapping("/{id}")
    public Post findById(@PathVariable Long id) {
        return postService.findById(id);
    }

    @GetMapping("/category/{category}")
    public List<Post> findByCategory(@PathVariable String category) {
        return postService.findByCategory(category);
    }

    @PostMapping
    public ResponseEntity<Post> create(@Valid @RequestBody PostRequest request) {
        Post post = postService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @PutMapping("/{id}")
    public Post update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
