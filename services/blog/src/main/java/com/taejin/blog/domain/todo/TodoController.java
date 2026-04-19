package com.taejin.blog.domain.todo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<TodoItem> findAll() {
        return todoService.findAll();
    }

    @GetMapping("/{id}")
    public TodoItem findById(@PathVariable Long id) {
        return todoService.findById(id);
    }

    @GetMapping("/phase/{phase}")
    public List<TodoItem> findByPhase(@PathVariable String phase) {
        return todoService.findByPhase(phase);
    }

    @GetMapping("/status/{status}")
    public List<TodoItem> findByStatus(@PathVariable TodoStatus status) {
        return todoService.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<TodoItem> create(@Valid @RequestBody TodoRequest request) {
        TodoItem item = todoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{id}")
    public TodoItem update(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        return todoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
