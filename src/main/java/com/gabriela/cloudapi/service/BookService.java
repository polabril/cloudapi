package com.gabriela.cloudapi.service;

import com.gabriela.cloudapi.model.Book;
import com.gabriela.cloudapi.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> getAll() {
        return repository.findAll();
    }

    public Book getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Book save(Book book) {
        return repository.save(book);
    }

    public Book update(Long id, Book book) {

        Book existing = getById(id);

        if (existing == null) return null;

        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());
        existing.setYear(book.getYear());
        existing.setIsbn(book.getIsbn());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}