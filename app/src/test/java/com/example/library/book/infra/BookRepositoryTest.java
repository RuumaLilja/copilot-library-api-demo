package com.example.library.book.infra;

import com.example.library.book.domain.BookStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
/**
 * BookRepositoryの基本的な永続化を検証する。
 */
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    /**
     * 対象：BookRepository + BookEntity（JPA永続化）
     * 観点：保存→採番→取得の基本動作、statusの永続化
     * 結果：bookIdが採番され、保存した各フィールドが取得できる
     */
    @Test
    void saveAndFind_shouldPersistEntity() {
        BookEntity entity = new BookEntity();
        entity.setTitle("テストタイトル");
        entity.setAuthor("テスト著者");
        entity.setStatus(BookStatus.AVAILABLE);

        BookEntity saved = bookRepository.save(entity);

        assertThat(saved.getBookId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(BookStatus.AVAILABLE);

        BookEntity found = bookRepository.findById(saved.getBookId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("テストタイトル");
        assertThat(found.getAuthor()).isEqualTo("テスト著者");
        assertThat(found.getStatus()).isEqualTo(BookStatus.AVAILABLE);
    }
}
