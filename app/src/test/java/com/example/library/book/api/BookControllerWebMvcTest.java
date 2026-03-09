package com.example.library.book.api;

import com.example.library.book.application.BookService;
import com.example.library.book.application.exception.BookAlreadyLoanedException;
import com.example.library.book.application.exception.BookNotFoundException;
import com.example.library.book.application.exception.BookNotLoanedException;
import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookStatus;
import com.example.library.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookController.class)
@Import(GlobalExceptionHandler.class)
class BookControllerWebMvcTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookService bookService;

        private static Book book(long bookId, String title, String author, BookStatus status) {
                return new Book(bookId, title, author, status);
        }

        /**
         * 対象：GET /api/v1/books?status=...
         * 観点：statusがenumに変換できない場合の入力不正
         * 結果：400 + ApiErrorResponse（code=INVALID_PARAMETER）を返す
         */
        @Test
        void listBooks_invalidStatus_returns400WithApiErrorResponse() throws Exception {
                mockMvc.perform(get("/api/v1/books").param("status", "INVALID"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
        }

        /**
         * 対象：POST /api/v1/books
         * 観点：Bean Validation（title/authorが空）
         * 結果：400 + ApiErrorResponse（code=VALIDATION_ERROR）を返す
         */
        @Test
        void createBook_validationError_returns400WithApiErrorResponse() throws Exception {
                String body = "{\"title\":\"\",\"author\":\"\"}";
                mockMvc.perform(post("/api/v1/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        /**
         * 対象：POST /api/v1/books
         * 観点：不正JSON（パース不可）
         * 結果：400 + ApiErrorResponse（code=MALFORMED_JSON）を返す
         */
        @Test
        void createBook_malformedJson_returns400WithApiErrorResponse() throws Exception {
                String body = "{";
                mockMvc.perform(post("/api/v1/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error.code").value("MALFORMED_JSON"));
        }

        /**
         * 対象：POST /api/v1/books
         * 観点：正常登録時のHTTP契約（201/レスポンス形）
         * 結果：201 + bookId/title/author/status を返す
         */
        @Test
        void createBook_success_returns201AndBody() throws Exception {
                when(bookService.createBook(eq("リーダブルコード"), eq("Dustin Boswell")))
                                .thenReturn(book(1L, "リーダブルコード", "Dustin Boswell", BookStatus.AVAILABLE));

                String body = "{\"title\":\"リーダブルコード\",\"author\":\"Dustin Boswell\"}";

                mockMvc.perform(post("/api/v1/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookId").value(1))
                                .andExpect(jsonPath("$.title").value("リーダブルコード"))
                                .andExpect(jsonPath("$.author").value("Dustin Boswell"))
                                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        /**
         * 対象：GET /api/v1/books
         * 観点：正常系のHTTP契約（200/配列レスポンス）
         * 結果：200 + BookResponse配列を返す
         */
        @Test
        void listBooks_success_returns200AndArray() throws Exception {
                when(bookService.listBooks(eq(Optional.empty())))
                                .thenReturn(List.of(
                                                book(1L, "A", "a", BookStatus.AVAILABLE),
                                                book(2L, "B", "b", BookStatus.LOANED)));

                mockMvc.perform(get("/api/v1/books"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].bookId").value(1))
                                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                                .andExpect(jsonPath("$[1].bookId").value(2))
                                .andExpect(jsonPath("$[1].status").value("LOANED"));
        }

        /**
         * 対象：GET /api/v1/books?status=AVAILABLE
         * 観点：クエリパラメータがServiceに正しく渡されること
         * 結果：Serviceが Optional.of(AVAILABLE) で呼ばれる
         */
        @Test
        void listBooks_withStatus_callsServiceWithOptionalStatus() throws Exception {
                when(bookService.listBooks(eq(Optional.of(BookStatus.AVAILABLE))))
                                .thenReturn(List.of(book(1L, "A", "a", BookStatus.AVAILABLE)));

                mockMvc.perform(get("/api/v1/books").param("status", "AVAILABLE"))
                                .andExpect(status().isOk());

                verify(bookService).listBooks(eq(Optional.of(BookStatus.AVAILABLE)));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/lend
         * 観点：貸出成功時のHTTP契約（200/レスポンス形）
         * 結果：200 + status=LOANED を返す
         */
        @Test
        void lend_success_returns200AndBody() throws Exception {
                when(bookService.lend(eq(1L)))
                                .thenReturn(book(1L, "A", "a", BookStatus.LOANED));

                mockMvc.perform(post("/api/v1/books/1/lend"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.bookId").value(1))
                                .andExpect(jsonPath("$.status").value("LOANED"));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/lend
         * 観点：存在しないbookIdのエラー変換
         * 結果：404 + ApiErrorResponse（code=BOOK_NOT_FOUND）を返す
         */
        @Test
        void lend_notFound_returns404WithApiError() throws Exception {
                when(bookService.lend(eq(404L))).thenThrow(new BookNotFoundException(404L));

                mockMvc.perform(post("/api/v1/books/404/lend"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error.code").value("BOOK_NOT_FOUND"))
                                .andExpect(jsonPath("$.error.details.bookId").value(404));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/lend
         * 観点：貸出中の本に対する貸出（ルール違反）のエラー変換
         * 結果：409 + ApiErrorResponse（code=BOOK_ALREADY_LOANED）を返す
         */
        @Test
        void lend_alreadyLoaned_returns409WithApiError() throws Exception {
                when(bookService.lend(eq(1L))).thenThrow(new BookAlreadyLoanedException(1L));

                mockMvc.perform(post("/api/v1/books/1/lend"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error.code").value("BOOK_ALREADY_LOANED"))
                                .andExpect(jsonPath("$.error.details.bookId").value(1));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/return
         * 観点：返却成功時のHTTP契約（200/レスポンス形）
         * 結果：200 + status=AVAILABLE を返す
         */
        @Test
        void returnBook_success_returns200AndBody() throws Exception {
                when(bookService.returnBook(eq(1L)))
                                .thenReturn(book(1L, "A", "a", BookStatus.AVAILABLE));

                mockMvc.perform(post("/api/v1/books/1/return"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.bookId").value(1))
                                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/return
         * 観点：存在しないbookIdのエラー変換
         * 結果：404 + ApiErrorResponse（code=BOOK_NOT_FOUND）を返す
         */
        @Test
        void returnBook_notFound_returns404WithApiError() throws Exception {
                when(bookService.returnBook(eq(404L))).thenThrow(new BookNotFoundException(404L));

                mockMvc.perform(post("/api/v1/books/404/return"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error.code").value("BOOK_NOT_FOUND"))
                                .andExpect(jsonPath("$.error.details.bookId").value(404));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/return
         * 観点：未貸出（AVAILABLE）への返却（ルール違反）のエラー変換
         * 結果：409 + ApiErrorResponse（code=BOOK_NOT_LOANED）を返す
         */
        @Test
        void returnBook_notLoaned_returns409WithApiError() throws Exception {
                when(bookService.returnBook(eq(1L))).thenThrow(new BookNotLoanedException(1L));

                mockMvc.perform(post("/api/v1/books/1/return"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error.code").value("BOOK_NOT_LOANED"))
                                .andExpect(jsonPath("$.error.details.bookId").value(1));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/lend
         * 観点：更新競合（楽観ロック等）のエラー変換
         * 結果：409 + ApiErrorResponse（code=BOOK_CONFLICT）を返す
         */
        @Test
        void optimisticLock_returns409WithApiError() throws Exception {
                when(bookService.lend(eq(1L))).thenThrow(new OptimisticLockingFailureException("conflict"));

                mockMvc.perform(post("/api/v1/books/1/lend"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error.code").value("BOOK_CONFLICT"));
        }

        /**
         * 対象：POST /api/v1/books/{bookId}/lend
         * 観点：パス変数bookIdの型変換エラー（数値でない）
         * 結果：400 + ApiErrorResponse（code=INVALID_PARAMETER）を返す
         */
        @Test
        void lend_invalidBookId_returns400WithApiErrorResponse() throws Exception {
                mockMvc.perform(post("/api/v1/books/abc/lend"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
        }

        /**
         * 対象：PUT /api/v1/books
         * 観点：許可されていないHTTPメソッド
         * 結果：405 + ApiErrorResponse（code=METHOD_NOT_ALLOWED）を返す
         */
        @Test
        void methodNotAllowed_returns405WithApiErrorResponse() throws Exception {
                mockMvc.perform(put("/api/v1/books"))
                                .andExpect(status().isMethodNotAllowed())
                                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
        }

        /**
         * 対象：POST /api/v1/books
         * 観点：Content-Type がサポートされない
         * 結果：415 + ApiErrorResponse（code=UNSUPPORTED_MEDIA_TYPE）を返す
         */
        @Test
        void unsupportedMediaType_returns415WithApiErrorResponse() throws Exception {
                mockMvc.perform(post("/api/v1/books")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("x"))
                                .andExpect(status().isUnsupportedMediaType())
                                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }

        /**
         * 対象：GET /api/v1/books
         * 観点：Accept がサポートされない
         * 結果：406 + ApiErrorResponse（code=NOT_ACCEPTABLE）を返す
         */
        @Test
        void notAcceptable_returns406WithApiErrorResponse() throws Exception {
                mockMvc.perform(get("/api/v1/books")
                                .accept(MediaType.APPLICATION_XML))
                                .andExpect(status().isNotAcceptable())
                                .andExpect(jsonPath("$.error.code").value("NOT_ACCEPTABLE"));
        }
}
