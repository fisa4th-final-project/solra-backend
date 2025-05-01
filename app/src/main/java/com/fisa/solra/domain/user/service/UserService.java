package com.fisa.solra.domain.user.service;

import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.user.dto.UserCreateRequestDto;
import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.dto.UserResponseDto;
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
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
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

    // 사용자 생성
    public UserResponseDto createUser(UserCreateRequestDto request){

        if (userRepository.existsByUserLoginId(request.getUserLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATED_LOGIN_ID);
        }

        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));

        Department dept = departmentRepository.findById(request.getDeptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        User user = User.builder()
                .userLoginId(request.getUserLoginId())
                .password(passwordEncoder.encode(request.getPassword())) // ✅ 비밀번호 암호화
                .userName(request.getUserName())
                .email(request.getEmail())
                .organization(org)
                .department(dept)
                .build();

        userRepository.save(user);

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userLoginId(user.getUserLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .organizationId(user.getOrganization().getOrgId())
                .departmentId(user.getDepartment().getDeptId())
                .build();
    }

}
