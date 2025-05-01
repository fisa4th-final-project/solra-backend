package com.fisa.solra.global.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {
    private final boolean success;
    private final int code;
    private final String message;
    private final Integer errorCode; // 실패 시 커스텀 에러코드, 성공 시 null
    private final T data;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .errorCode(null)
                .data(data)
                .build();
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(int statusCode, String message, int errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(statusCode)
                .message(message)
                .errorCode(errorCode)
                .data(null)
                .build();
    }
}
