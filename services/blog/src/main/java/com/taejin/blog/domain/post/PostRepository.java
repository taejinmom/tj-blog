package com.taejin.blog.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategory(String category);

    List<Post> findAllByOrderByCreatedAtDesc();
}
