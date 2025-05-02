package com.fisa.solra.global.auth;

import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpSession;

public class AuthUtil {

    /**
     * 세션에 저장된 JWT 토큰에서 role을 추출하여 ROOT 권한 여부를 검증합니다.
     * ROOT가 아니면 ACCESS_DENIED 예외를 발생시킵니다.
     */
    public static void assertRoot(HttpSession session, JwtTokenProvider jwtTokenProvider) {
        String token = (String) session.getAttribute("jwtToken");

        if (token == null || !"ROOT".equals(jwtTokenProvider.getRole(token))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
