package com.taejin.blog.domain.todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    List<TodoItem> findByPhase(String phase);

    List<TodoItem> findByStatus(TodoStatus status);

    List<TodoItem> findAllByOrderByPhaseAscIdAsc();
}
