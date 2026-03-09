package com.example.library.book.application.exception;

/**
 * 指定した本が存在しないことを表す。
 */
public class BookNotFoundException extends RuntimeException {

    private final long bookId;

    /**
     * 例外を作成する。
     */
    public BookNotFoundException(long bookId) {
        super("Book not found: bookId=" + bookId);
        this.bookId = bookId;
    }

    public long getBookId() {
        return bookId;
    }
}
