package com.gabriela.cloudapi.controller;

import com.gabriela.cloudapi.model.Book;
import com.gabriela.cloudapi.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Book> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable Long id) {

        Book book = service.getById(id);

        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Book book) {
        return ResponseEntity.ok(service.save(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> update(
            @PathVariable Long id,
            @RequestBody Book book) {

        Book updated = service.update(id, book);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}