package com.example.library.book.infra;

import com.example.library.book.domain.BookStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 本の永続化操作を提供する。
 */
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    /**
     * 状態で本を検索する。
     */
    List<BookEntity> findByStatus(BookStatus status);
}
