package com.fisa.solra.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Around;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class LoggingAspect {


    // 모든 Controller의 메서드에 적용
    @Around("execution(* com.fisa.solra.domain..controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        // HTTP 요청 정보 획득
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = sra.getRequest();

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.getRemoteAddr();
            if ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
                clientIp = "127.0.0.1";
            }
        }

        // 사용자 정보
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null && authentication.getName() != null)
                ? authentication.getName() : "anonymous";
        String authorities = (authentication != null) ? authentication.getAuthorities().toString() : "N/A";

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ 사용자 요청 | ID: {} | ROLE: {} | IP: {} | URI: {} | 메서드: {} | 결과: 성공 | 처리시간: {}ms",
                    username, authorities, clientIp, uri, method, duration);
            return result;
        } catch (Throwable t) {
            log.warn("❌ 사용자 요청 실패 | ID: {} | ROLE: {} | IP: {} | URI: {} | 메서드: {} | 에러: {}",
                    username, authorities, clientIp, uri, method, t.getMessage());
            throw t;
        }
    }
}
