package com.fisa.solra.global.util;

import com.fisa.solra.global.security.UserPrincipal;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class SecurityUtil {

    public static UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessException(ErrorCode.JWT_TOKEN_NOT_FOUND);
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUserPrincipal().getUserId();
    }

    public static Long getOrgId() {
        return getCurrentUserPrincipal().getOrgId();
    }

    public static Long getDeptId() {
        return getCurrentUserPrincipal().getDeptId();
    }

    public static List<String> getRoles() {
        return getCurrentUserPrincipal().getRoles();
    }

    public static boolean hasRole(String roleName) {
        return getRoles().contains(roleName);
    }
}
