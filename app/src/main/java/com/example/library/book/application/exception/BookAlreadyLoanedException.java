package com.example.library.book.application.exception;

/**
 * 本が貸出中であることを表す。
 */
public class BookAlreadyLoanedException extends RuntimeException {

    private final long bookId;

    /**
     * 例外を作成する。
     */
    public BookAlreadyLoanedException(long bookId) {
        super("Book is already loaned: bookId=" + bookId);
        this.bookId = bookId;
    }

    public long getBookId() {
        return bookId;
    }
}
