package com.taejin.blog.domain.post;

import com.taejin.blog.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 페이지네이션 + 검색(q) + 카테고리/태그 필터.
     * 우선순위: tag → q(키워드) → category → 전체.
     * null 파라미터를 쿼리에 넘기지 않도록 분기한다(Postgres 타입추론 오류 방지).
     */
    public Page<Post> list(String q, String category, String tag, Pageable pageable) {
        if (hasText(tag)) {
            return postRepository.findByTag(tag.trim(), pageable);
        }
        if (hasText(q)) {
            return postRepository.searchByKeyword(q.trim(), pageable);
        }
        if (hasText(category)) {
            return postRepository.findByCategory(category.trim(), pageable);
        }
        return postRepository.findAll(pageable);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found: " + id));
    }

    public List<Post> findByCategory(String category) {
        return postRepository.findByCategory(category);
    }

    @Transactional
    public Post create(PostRequest request) {
        Post post = new Post(
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.getTags()
        );
        return postRepository.save(post);
    }

    @Transactional
    public Post update(Long id, PostRequest request) {
        Post post = findById(id);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setTags(request.getTags());
        return postRepository.save(post);
    }

    @Transactional
    public void delete(Long id) {
        postRepository.deleteById(id);
    }
}
