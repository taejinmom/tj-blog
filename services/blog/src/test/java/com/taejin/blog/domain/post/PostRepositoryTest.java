package com.taejin.blog.domain.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    PostRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new Post("Spring Boot 입문", "JPA와 Spring 이야기", "Tech", List.of("spring", "jpa")));
        repository.save(new Post("Docker 정리", "컨테이너 기초", "DevOps", List.of("docker")));
        repository.save(new Post("React 훅", "useEffect 패턴", "Tech", List.of()));
    }

    @Test
    void 페이지네이션_크기와_총개수() {
        Page<Post> page = repository.findAll(PageRequest.of(0, 2, Sort.by("createdAt").descending()));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void 키워드_검색_대소문자무시_제목과내용() {
        assertThat(repository.searchByKeyword("docker", PageRequest.of(0, 10)).getContent())
                .extracting(Post::getTitle).containsExactly("Docker 정리");
        // 내용에만 있는 키워드도 매칭
        assertThat(repository.searchByKeyword("useEffect", PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void 카테고리_필터() {
        assertThat(repository.findByCategory("Tech", PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    void 태그_필터() {
        assertThat(repository.findByTag("jpa", PageRequest.of(0, 10)).getContent())
                .extracting(Post::getTitle).containsExactly("Spring Boot 입문");
        assertThat(repository.findByTag("none", PageRequest.of(0, 10)).getTotalElements())
                .isZero();
    }
}
