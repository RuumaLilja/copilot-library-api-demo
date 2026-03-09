package com.example.library.book.application;

import com.example.library.book.application.exception.BookAlreadyLoanedException;
import com.example.library.book.application.exception.BookNotFoundException;
import com.example.library.book.application.exception.BookNotLoanedException;
import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookStatus;
import com.example.library.book.infra.BookEntity;
import com.example.library.book.infra.BookRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 図書の登録・一覧・貸出・返却のユースケースを提供する。
 */
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * 本を登録する。
     *
     * <p>
     * 登録直後の状態は常に {@link BookStatus#AVAILABLE} とする。
     * </p>
     */
    @Transactional
    public Book createBook(String title, String author) {
        BookEntity entity = new BookEntity();
        entity.setTitle(title);
        entity.setAuthor(author);
        entity.setStatus(BookStatus.AVAILABLE);
        return toModel(bookRepository.save(entity));
    }

    /**
     * 本の一覧を取得する。
     *
     * <p>
     * 状態が指定されていれば絞り込む。
     * </p>
     */
    @Transactional(readOnly = true)
    public List<Book> listBooks(Optional<BookStatus> status) {
        List<BookEntity> entities;
        if (status.isPresent()) {
            entities = bookRepository.findByStatus(status.get());
        } else {
            entities = bookRepository.findAll(Sort.by(Sort.Direction.ASC, "bookId"));
        }
        return entities.stream().map(BookService::toModel).toList();
    }

    /**
     * 本を貸し出す。
     *
     * @throws BookNotFoundException      本が存在しない場合
     * @throws BookAlreadyLoanedException 貸出中の場合
     */
    @Transactional
    public Book lend(long bookId) {
        BookEntity entity = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        if (entity.getStatus() != BookStatus.AVAILABLE) {
            throw new BookAlreadyLoanedException(bookId);
        }
        entity.setStatus(BookStatus.LOANED);
        return toModel(entity);
    }

    /**
     * 本を返却する。
     *
     * @throws BookNotFoundException  本が存在しない場合
     * @throws BookNotLoanedException 貸出中ではない場合
     */
    @Transactional
    public Book returnBook(long bookId) {
        BookEntity entity = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        if (entity.getStatus() != BookStatus.LOANED) {
            throw new BookNotLoanedException(bookId);
        }
        entity.setStatus(BookStatus.AVAILABLE);
        return toModel(entity);
    }

    private static Book toModel(BookEntity entity) {
        Long bookId = entity.getBookId();
        if (bookId == null) {
            throw new IllegalStateException("bookId is not assigned");
        }
        return new Book(bookId, entity.getTitle(), entity.getAuthor(), entity.getStatus());
    }
}
