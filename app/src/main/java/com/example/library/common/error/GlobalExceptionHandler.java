package com.example.library.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.library.book.application.exception.BookAlreadyLoanedException;
import com.example.library.book.application.exception.BookNotFoundException;
import com.example.library.book.application.exception.BookNotLoanedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 例外をエラー応答へ変換する。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * バリデーションエラーを変換する。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        details.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(new ApiError("VALIDATION_ERROR", "入力値が不正です", details)));
    }

    /**
     * パラメータ型変換エラー（例: status が enum に変換できない、bookId が数値でない）を変換する。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("name", ex.getName());
        details.put("value", ex.getValue());
        if (ex.getRequiredType() != null) {
            details.put("expectedType", ex.getRequiredType().getSimpleName());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(new ApiError("INVALID_PARAMETER", "パラメータが不正です", details)));
    }

    /**
     * JSONパースエラー等（例: 不正JSON、必須ボディ欠落）を変換する。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(new ApiError("MALFORMED_JSON", "リクエストボディが不正です", Map.of())));
    }

    /**
     * 許可されていないHTTPメソッド（405）を変換する。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("method", ex.getMethod());

        Set<HttpMethod> supportedHttpMethods = ex.getSupportedHttpMethods();
        if (supportedHttpMethods != null) {
            List<String> supported = supportedHttpMethods.stream()
                    .map(HttpMethod::name)
                    .sorted()
                    .toList();
            details.put("supportedMethods", supported);
        } else if (ex.getSupportedMethods() != null) {
            details.put("supportedMethods", Arrays.asList(ex.getSupportedMethods()));
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponse(new ApiError("METHOD_NOT_ALLOWED", "許可されていないメソッドです", details)));
    }

    /**
     * サポートされないContent-Type（415）を変換する。
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        Map<String, Object> details = new HashMap<>();
        if (ex.getContentType() != null) {
            details.put("contentType", ex.getContentType().toString());
        }
        details.put("supportedMediaTypes", ex.getSupportedMediaTypes().stream().map(MediaType::toString).toList());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiErrorResponse(
                        new ApiError("UNSUPPORTED_MEDIA_TYPE", "Content-Typeがサポートされていません", details)));
    }

    /**
     * サポートされないAccept（406）を変換する。
     *
     * Acceptヘッダの内容によっては、通常のコンテンツネゴシエーション経由だとボディが書けないため、
     * JSONをレスポンスへ直接書き込む。
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Map<String, Object> details = new HashMap<>();
        String accept = request.getHeader("Accept");
        if (accept != null) {
            details.put("accept", accept);
        }
        details.put("supportedMediaTypes", ex.getSupportedMediaTypes().stream().map(MediaType::toString).toList());

        ApiErrorResponse body = new ApiErrorResponse(
                new ApiError("NOT_ACCEPTABLE", "Acceptヘッダがサポートされていません", details));

        response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * 未存在エラーを変換する。
     */
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(BookNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        new ApiError("BOOK_NOT_FOUND", "本が見つかりません", Map.of("bookId", ex.getBookId()))));
    }

    /**
     * 貸出中エラーを変換する。
     */
    @ExceptionHandler(BookAlreadyLoanedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyLoaned(BookAlreadyLoanedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiError("BOOK_ALREADY_LOANED", "貸出中のため貸出できません", Map.of("bookId", ex.getBookId()))));
    }

    /**
     * 未貸出エラーを変換する。
     */
    @ExceptionHandler(BookNotLoanedException.class)
    public ResponseEntity<ApiErrorResponse> handleNotLoaned(BookNotLoanedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiError("BOOK_NOT_LOANED", "貸出中ではないため返却できません", Map.of("bookId", ex.getBookId()))));
    }

    /**
     * 更新競合を変換する。
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(new ApiError("BOOK_CONFLICT", "他の更新と競合しました。再試行してください", Map.of())));
    }

    /**
     * 想定外の例外を変換する（詳細は返さずログに出す）。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(RuntimeException ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(new ApiError("INTERNAL_SERVER_ERROR", "予期しないエラーが発生しました", Map.of())));
    }
}
