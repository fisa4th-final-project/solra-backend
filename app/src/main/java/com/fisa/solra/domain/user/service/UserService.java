package com.fisa.solra.domain.user.service;

import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserLoginInfo login(String userLoginId, String password) {
        User user = userRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return UserLoginInfo.builder()
                .userId(user.getUserId())
                .orgId(user.getOrganization().getOrgId())
                .deptId(user.getDepartment().getDeptId())
                .role("USER") // 또는 user.getRole().getRoleName()
                .build();
    }

}
