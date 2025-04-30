package com.fisa.solra.global.exception;

import com.fisa.solra.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode error = e.getErrorCode();
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.fail(error.getStatus().value(), error.getMessage(), error.getCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        e.printStackTrace();
        ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.fail(error.getStatus().value(), error.getMessage(), error.getCode()));
    }

}
