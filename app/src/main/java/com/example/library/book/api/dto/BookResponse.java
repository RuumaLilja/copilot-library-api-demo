package com.example.library.book.api.dto;

import com.example.library.book.domain.BookStatus;

/**
 * 本の情報を返す。
 */
public class BookResponse {

    private final long bookId;
    private final String title;
    private final String author;
    private final BookStatus status;

    /**
     * レスポンスを作成する。
     */
    public BookResponse(long bookId, String title, String author, BookStatus status) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.status = status;
    }

    public long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public BookStatus getStatus() {
        return status;
    }
}
