package com.booksquest.api.book.repository;

import com.booksquest.shared.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("bookModuleBookRepository")
public interface BookRepository extends JpaRepository<Book, Long> {
    @Deprecated
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);
}
