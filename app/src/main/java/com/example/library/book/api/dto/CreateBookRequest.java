package com.example.library.book.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 本の登録リクエストを表す。
 */
public class CreateBookRequest {

    /** タイトル。 */
    @NotBlank
    private String title;

    /** 著者名。 */
    @NotBlank
    private String author;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
