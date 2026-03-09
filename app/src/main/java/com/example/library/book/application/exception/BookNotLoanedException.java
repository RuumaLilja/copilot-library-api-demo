package com.example.library.book.application.exception;

/**
 * 本が貸出中ではないことを表す。
 */
public class BookNotLoanedException extends RuntimeException {

    private final long bookId;

    /**
     * 例外を作成する。
     */
    public BookNotLoanedException(long bookId) {
        super("Book is not loaned: bookId=" + bookId);
        this.bookId = bookId;
    }

    public long getBookId() {
        return bookId;
    }
}
