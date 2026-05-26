package com.taejin.blog.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategory(String category);

    List<Post> findAllByOrderByCreatedAtDesc();

    /** 카테고리별 페이지 조회. */
    Page<Post> findByCategory(String category, Pageable pageable);

    /**
     * 제목/내용 키워드 검색. (q 는 호출 측에서 non-null 보장 — Postgres 에서
     * null 파라미터를 함수에 넘기면 bytea 로 추론돼 오류가 나므로 분기 처리한다.)
     */
    @Query("""
            select p from Post p
            where lower(p.title) like lower(concat('%', :q, '%'))
               or lower(p.content) like lower(concat('%', :q, '%'))
            """)
    Page<Post> searchByKeyword(@Param("q") String q, Pageable pageable);

    /** 특정 태그를 가진 게시물 페이지 조회 (컬렉션 조인 대신 member of 사용). */
    @Query("select p from Post p where :tag member of p.tags")
    Page<Post> findByTag(@Param("tag") String tag, Pageable pageable);
}
