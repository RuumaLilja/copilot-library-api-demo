package com.example.library.book.api;

import com.example.library.book.api.dto.BookResponse;
import com.example.library.book.api.dto.CreateBookRequest;
import com.example.library.book.application.BookService;
import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 図書に関するREST APIを提供する。
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 本を登録する。
     */
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        Book created = bookService.createBook(request.getTitle(), request.getAuthor());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /**
     * 本の一覧を取得する。
     */
    @GetMapping
    public ResponseEntity<List<BookResponse>> listBooks(@RequestParam(required = false) BookStatus status) {
        List<BookResponse> list = bookService.listBooks(Optional.ofNullable(status)).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * 本を貸し出す。
     */
    @PostMapping("/{bookId}/lend")
    public ResponseEntity<BookResponse> lend(@PathVariable long bookId) {
        return ResponseEntity.ok(toResponse(bookService.lend(bookId)));
    }

    /**
     * 本を返却する。
     */
    @PostMapping("/{bookId}/return")
    public ResponseEntity<BookResponse> returnBook(@PathVariable long bookId) {
        return ResponseEntity.ok(toResponse(bookService.returnBook(bookId)));
    }

    /**
     * ドメインモデルをレスポンスへ変換する。
     */
    private BookResponse toResponse(Book book) {
        return new BookResponse(book.bookId(), book.title(), book.author(), book.status());
    }
}
