package com.fisa.solra.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ✅ 1000번대: 인증/인가
    UNAUTHENTICATED(1001, HttpStatus.UNAUTHORIZED, "로그인 상태가 아닙니다. (세션 없음 또는 만료)"),
    INVALID_CREDENTIALS(1002, HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    ACCESS_DENIED(1003, HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다."),

    // ✅ 2000번대: 사용자
    DUPLICATED_LOGIN_ID(2001, HttpStatus.BAD_REQUEST, "이미 존재하는 로그인 ID입니다."),
    DUPLICATED_EMAIL(2002, HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(2003, HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    USER_ALREADY_HAS_PERMISSION(2004, HttpStatus.BAD_REQUEST, "해당 권한은 이미 사용자에게 부여되어 있습니다."),
    USER_PERMISSION_NOT_FOUND(2005, HttpStatus.NOT_FOUND, "사용자에게 해당 권한이 존재하지 않습니다."),
    USER_LOGIN_ID_DUPLICATED(2006, HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),



    // ✅ 3000번대: 조직 / 부서
    ORGANIZATION_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "소속된 조직을 찾을 수 없습니다."),
    DUPLICATED_DEPARTMENT_NAME(3002, HttpStatus.BAD_REQUEST, "동일한 이름의 부서가 이미 존재합니다."),
    DEPARTMENT_NOT_FOUND(3003, HttpStatus.NOT_FOUND, "해당 부서를 찾을 수 없습니다."),
    ORGANIZATION_DELETE_FAILED(3004, HttpStatus.BAD_REQUEST, "조직에 속한 자식 리소스가 있어 삭제할 수 없습니다."),
    INVALID_INPUT(3005, HttpStatus.BAD_REQUEST, "입력 값이 유효하지 않습니다."),
    DUPLICATED_ORGANIZATION_NAME(3006, HttpStatus.CONFLICT, "동일한 이름의 조직이 이미 존재합니다."),
    ORGANIZATION_CREATE_FAILED(3007, HttpStatus.BAD_REQUEST, "조직 생성에 실패했습니다."),
    DEPARTMENT_CREATE_FAILED(3008, HttpStatus.BAD_REQUEST, "부서 생성에 실패했습니다."),
    DEPARTMENT_DELETE_FAILED(3009, HttpStatus.BAD_REQUEST, "부서 삭제에 실패했습니다."),

    // ✅ 조직 / 부서 삭제 충돌 (FK 제약 조건 위반)
    ORGANIZATION_DELETE_CONFLICT(3100, HttpStatus.CONFLICT, "해당 조직은 부서 또는 사용자와 연결되어 있어 삭제할 수 없습니다."),
    DEPARTMENT_DELETE_CONFLICT(3200, HttpStatus.CONFLICT, "해당 부서는 사용자와 연결되어 있어 삭제할 수 없습니다."),

    // ✅ 4000번대: 클러스터
    CLUSTER_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "클러스터를 찾을 수 없습니다."),
    CLUSTER_CREATION_FAILED(4002, HttpStatus.BAD_REQUEST, "클러스터 생성에 실패했습니다."),
    DUPLICATED_CLUSTER_NAME(4003, HttpStatus.CONFLICT, "동일한 이름의 클러스터가 이미 존재합니다."),

    // ✅ 5000번대: 권한 / 역할
    DUPLICATED_PERMISSION_NAME(5001, HttpStatus.BAD_REQUEST, "이미 존재하는 권한 이름입니다."),
    PERMISSION_NOT_FOUND(5002, HttpStatus.NOT_FOUND, "요청한 권한이 존재하지 않습니다."),
    ROLE_ALREADY_ASSIGNED(5003, HttpStatus.CONFLICT, "해당 역할은 이미 사용자에게 부여되어 있습니다."),
    ROLE_NOT_FOUND(5004, HttpStatus.NOT_FOUND, "요청한 역할이 존재하지 않습니다."),
    PERMISSION_DELETE_FAILED(5005, HttpStatus.BAD_REQUEST, "권한 삭제에 실패했습니다."),
    ROLE_PERMISSION_NOT_FOUND(5006, HttpStatus.CONFLICT, "역할 권한 매핑을 찾을 수 없습니다."),
    PERMISSION_ALREADY_ASSIGNED(5007, HttpStatus.CONFLICT, "해당 권한은 이미 사용자에게 부여되어 있습니다."),

    // ✅ 역할 / 권한 삭제 충돌 (FK 제약 조건 위반)
    ROLE_DELETE_CONFLICT(5100, HttpStatus.CONFLICT, "해당 역할은 사용자 또는 권한과 연결되어 있어 삭제할 수 없습니다."),
    PERMISSION_DELETE_CONFLICT(5101, HttpStatus.CONFLICT, "해당 권한은 사용자 또는 역할과 연결되어 있어 삭제할 수 없습니다."),

    // ✅ 9000번대: 시스템
    INTERNAL_SERVER_ERROR(9000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    UNKNOWN_EXCEPTION(9001, HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 알 수 없는 예외가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

}
