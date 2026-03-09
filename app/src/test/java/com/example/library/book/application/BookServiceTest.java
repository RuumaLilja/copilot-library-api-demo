package com.example.library.book.application;

import com.example.library.book.application.exception.BookAlreadyLoanedException;
import com.example.library.book.application.exception.BookNotFoundException;
import com.example.library.book.application.exception.BookNotLoanedException;
import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookStatus;
import com.example.library.book.infra.BookEntity;
import com.example.library.book.infra.BookRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(BookService.class)
class BookServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 対象：BookService.createBook
     * 観点：登録直後の状態が常にAVAILABLEであること、採番されること
     * 結果：bookIdが採番され、status=AVAILABLEで永続化される
     */
    @Test
    void createBook_assignsIdAndAlwaysAvailable() {
        Book created = bookService.createBook("リーダブルコード", "Dustin Boswell");

        assertThat(created.bookId()).isPositive();
        assertThat(created.status()).isEqualTo(BookStatus.AVAILABLE);

        bookRepository.flush();
        entityManager.clear();

        BookEntity found = bookRepository.findById(created.bookId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("リーダブルコード");
        assertThat(found.getAuthor()).isEqualTo("Dustin Boswell");
        assertThat(found.getStatus()).isEqualTo(BookStatus.AVAILABLE);
    }

    /**
     * 対象：BookService.listBooks（status未指定）
     * 観点：全件取得でき、bookId昇順で返ること
     * 結果：登録した全件が昇順で取得できる
     */
    @Test
    void listBooks_withoutStatus_returnsAllSortedByBookIdAsc() {
        Book b1 = bookService.createBook("A", "a");
        Book b2 = bookService.createBook("B", "b");
        Book b3 = bookService.createBook("C", "c");

        List<Book> list = bookService.listBooks(Optional.empty());

        assertThat(list).hasSize(3);
        assertThat(list.stream().map(Book::bookId).toList())
                .containsExactly(b1.bookId(), b2.bookId(), b3.bookId());
    }

    /**
     * 対象：BookService.listBooks（status指定）
     * 観点：statusで絞り込みできること
     * 結果：AVAILABLE/LOANEDそれぞれ指定したものだけが取得できる
     */
    @Test
    void listBooks_withStatus_filtersByStatus() {
        Book b1 = bookService.createBook("A", "a");
        Book b2 = bookService.createBook("B", "b");
        Book b3 = bookService.createBook("C", "c");
        bookService.lend(b2.bookId());

        bookRepository.flush();
        entityManager.clear();

        List<Book> available = bookService.listBooks(Optional.of(BookStatus.AVAILABLE));
        List<Book> loaned = bookService.listBooks(Optional.of(BookStatus.LOANED));

        assertThat(available.stream().map(Book::bookId).toList())
                .containsExactlyInAnyOrder(b1.bookId(), b3.bookId());
        assertThat(loaned.stream().map(Book::bookId).toList())
                .containsExactly(b2.bookId());
    }

    /**
     * 対象：BookService.lend
     * 観点：AVAILABLEからLOANEDへ状態遷移できること
     * 結果：statusがLOANEDへ更新され永続化される
     */
    @Test
    void lend_availableToLoaned_updatesStatus() {
        Book created = bookService.createBook("A", "a");

        Book updated = bookService.lend(created.bookId());
        assertThat(updated.status()).isEqualTo(BookStatus.LOANED);

        bookRepository.flush();
        entityManager.clear();

        BookEntity found = bookRepository.findById(created.bookId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(BookStatus.LOANED);
    }

    /**
     * 対象：BookService.lend
     * 観点：すでにLOANEDの本を再貸出できないこと
     * 結果：BookAlreadyLoanedExceptionが送出される
     */
    @Test
    void lend_whenAlreadyLoaned_throwsBookAlreadyLoanedException() {
        Book created = bookService.createBook("A", "a");
        bookService.lend(created.bookId());

        assertThatThrownBy(() -> bookService.lend(created.bookId()))
                .isInstanceOf(BookAlreadyLoanedException.class);
    }

    /**
     * 対象：BookService.lend
     * 観点：存在しないbookIdの扱い
     * 結果：BookNotFoundExceptionが送出される
     */
    @Test
    void lend_whenNotFound_throwsBookNotFoundException() {
        assertThatThrownBy(() -> bookService.lend(9999L))
                .isInstanceOf(BookNotFoundException.class);
    }

    /**
     * 対象：BookService.returnBook
     * 観点：LOANEDからAVAILABLEへ状態遷移できること
     * 結果：statusがAVAILABLEへ更新され永続化される
     */
    @Test
    void returnBook_loanedToAvailable_updatesStatus() {
        Book created = bookService.createBook("A", "a");
        bookService.lend(created.bookId());

        Book updated = bookService.returnBook(created.bookId());
        assertThat(updated.status()).isEqualTo(BookStatus.AVAILABLE);

        bookRepository.flush();
        entityManager.clear();

        BookEntity found = bookRepository.findById(created.bookId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(BookStatus.AVAILABLE);
    }

    /**
     * 対象：BookService.returnBook
     * 観点：AVAILABLEの本を返却できないこと
     * 結果：BookNotLoanedExceptionが送出される
     */
    @Test
    void returnBook_whenNotLoaned_throwsBookNotLoanedException() {
        Book created = bookService.createBook("A", "a");

        assertThatThrownBy(() -> bookService.returnBook(created.bookId()))
                .isInstanceOf(BookNotLoanedException.class);
    }

    /**
     * 対象：BookService.returnBook
     * 観点：存在しないbookIdの扱い
     * 結果：BookNotFoundExceptionが送出される
     */
    @Test
    void returnBook_whenNotFound_throwsBookNotFoundException() {
        assertThatThrownBy(() -> bookService.returnBook(9999L))
                .isInstanceOf(BookNotFoundException.class);
    }

    /**
     * 対象：BookEntity（@Version）による楽観ロック
     * 観点：古いversionで更新した場合に競合を検知できること
     * 結果：OptimisticLockingFailureExceptionが送出される
     */
    @Test
    @DirtiesContext
    void optimisticLock_conflictingUpdate_throwsOptimisticLockingFailureException() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long bookId = requiresNew.execute(status -> {
            BookEntity entity = new BookEntity();
            entity.setTitle("A");
            entity.setAuthor("a");
            entity.setStatus(BookStatus.AVAILABLE);
            BookEntity saved = bookRepository.save(entity);
            bookRepository.flush();
            return saved.getBookId();
        });

        BookEntity stale = requiresNew.execute(status -> {
            BookEntity loaded = bookRepository.findById(bookId).orElseThrow();
            entityManager.detach(loaded);
            return loaded;
        });

        requiresNew.executeWithoutResult(status -> {
            BookEntity loaded = bookRepository.findById(bookId).orElseThrow();
            loaded.setStatus(BookStatus.LOANED);
            bookRepository.flush();
        });

        assertThatThrownBy(() -> requiresNew.executeWithoutResult(status -> {
            stale.setStatus(BookStatus.AVAILABLE);
            bookRepository.save(stale);
            bookRepository.flush();
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
