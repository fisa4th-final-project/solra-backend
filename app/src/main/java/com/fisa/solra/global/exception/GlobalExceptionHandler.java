package com.fisa.solra.global.exception;

import com.fisa.solra.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode error = e.getErrorCode();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        String roles = (auth != null) ? auth.getAuthorities().toString() : "N/A";
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.getRemoteAddr();
            if ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
                clientIp = "127.0.0.1";
            }
        }

        log.warn("❌ 권한 실패 | ID: {} | ROLE: {} | IP: {} | URI: {} | 메서드: {} | 에러: {}",
                userId, roles, clientIp, uri, method, e.getMessage());

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String rootMessage = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
        log.warn("데이터 무결성 위반 감지: {}", rootMessage);

        if (rootMessage != null) {
            if (rootMessage.contains("user_role")) {
                return buildError(ErrorCode.ROLE_DELETE_CONFLICT);
            }
            if (rootMessage.contains("user_permission") || rootMessage.contains("role_permission")) {
                return buildError(ErrorCode.PERMISSION_DELETE_CONFLICT);
            }
            if (rootMessage.contains("department") || rootMessage.contains("user") && rootMessage.contains("organization")) {
                return buildError(ErrorCode.ORGANIZATION_DELETE_CONFLICT);
            }
            if (rootMessage.contains("user") && rootMessage.contains("department")) {
                return buildError(ErrorCode.DEPARTMENT_DELETE_CONFLICT);
            }
        }

        return buildError(ErrorCode.INTERNAL_SERVER_ERROR, "데이터 무결성 제약 위반");
    }

    private ResponseEntity<ApiResponse<Void>> buildError(ErrorCode error) {
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.fail(error.getStatus().value(), error.getMessage(), error.getCode()));
    }

    private ResponseEntity<ApiResponse<Void>> buildError(ErrorCode error, String overrideMessage) {
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.fail(error.getStatus().value(), overrideMessage, error.getCode()));
    }

    @ExceptionHandler(io.fabric8.kubernetes.client.KubernetesClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleK8sConnectionError(io.fabric8.kubernetes.client.KubernetesClientException e) {
        log.error("Kubernetes API 연결 오류", e);
        ErrorCode error = ErrorCode.CLUSTER_CONNECTION_FAILED;
        return ResponseEntity
                .status(error.getStatus())
                .body(ApiResponse.fail(
                        error.getStatus().value(),
                        error.getMessage(),
                        error.getCode()
                ));
    }
}
