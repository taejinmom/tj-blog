package com.taejin.blog.domain.todo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public List<TodoItem> findAll() {
        return todoRepository.findAllByOrderByPhaseAscIdAsc();
    }

    public TodoItem findById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TodoItem not found: " + id));
    }

    public List<TodoItem> findByPhase(String phase) {
        return todoRepository.findByPhase(phase);
    }

    public List<TodoItem> findByStatus(TodoStatus status) {
        return todoRepository.findByStatus(status);
    }

    @Transactional
    public TodoItem create(TodoRequest request) {
        TodoItem item = new TodoItem(
                request.getTitle(),
                request.getDescription(),
                request.getStatus() != null ? request.getStatus() : TodoStatus.PENDING,
                request.getPhase()
        );
        return todoRepository.save(item);
    }

    @Transactional
    public TodoItem update(Long id, TodoRequest request) {
        TodoItem item = findById(id);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        item.setPhase(request.getPhase());
        return todoRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.deleteById(id);
    }
}
