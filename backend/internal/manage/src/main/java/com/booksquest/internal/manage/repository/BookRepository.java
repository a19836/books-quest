package com.booksquest.internal.manage.repository;

import com.booksquest.shared.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("manageModuleBookRepository")
public interface BookRepository extends JpaRepository<Book, Long> {
}
