package com.example.library.common.error;

/**
 * APIエラー応答のルート要素を表す。
 */
public class ApiErrorResponse {

    private final ApiError error;

    /**
     * エラー応答を作成する。
     */
    public ApiErrorResponse(ApiError error) {
        this.error = error;
    }

    public ApiError getError() {
        return error;
    }
}
