package com.example.library.book.domain;

/**
 * 本を表すドメインモデル。
 */
public record Book(
        long bookId,
        String title,
        String author,
        BookStatus status) {
}
