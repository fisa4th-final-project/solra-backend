package com.fisa.solra.domain.user.service;

import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.user.dto.UserCreateRequestDto;
import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.dto.UserUpdateRequestDto;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

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

        // Null 체크 추가: 조직/부서가 없을 수 있음
        Long orgId = user.getOrganization() != null ? user.getOrganization().getOrgId() : null;
        Long deptId = user.getDepartment() != null ? user.getDepartment().getDeptId() : null;

        return UserLoginInfo.builder()
                .userId(user.getUserId())
                .orgId(orgId)
                .deptId(deptId)
                .roles(user.getRoles()) // 또는 user.getRole().getRoleName()
                .build();
    }

    // 사용자 생성
    public UserResponseDto createUser(UserCreateRequestDto request){

        if (userRepository.existsByUserLoginId(request.getUserLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATED_LOGIN_ID);
        }

        // 조직 ID가 null이 아닐 경우만 조회, null 허용
        Organization org = null;
        if (request.getOrgId() != null) {
            org = organizationRepository.findById(request.getOrgId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));
        }

        // 부서 ID가 null이 아닐 경우만 조회, null 허용
        Department dept = null;
        if (request.getDeptId() != null) {
            dept = departmentRepository.findById(request.getDeptId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

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
                .organizationId(user.getOrganization() != null ? user.getOrganization().getOrgId() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null)
                .build();
    }

    // 사용자 조회
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 조직과 부서가 null일 수 있으므로 안전하게 추출
        Long orgId = user.getOrganization() != null ? user.getOrganization().getOrgId() : null;
        Long deptId = user.getDepartment() != null ? user.getDepartment().getDeptId() : null;

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userLoginId(user.getUserLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .organizationId(orgId)
                .departmentId(deptId)
                .build();
    }

    // 사용자 수정
    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // userLoginId 중복 검사 (현재 user 제외)
        if (requestDto.getUserLoginId() != null &&
                !requestDto.getUserLoginId().equals(user.getUserLoginId()) &&
                userRepository.existsByUserLoginId(requestDto.getUserLoginId())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATED);
        }

        // 이메일 중복 검사 (자기 자신 제외)
        boolean emailTaken = userRepository.existsByEmailAndUserIdNot(requestDto.getEmail(), userId);
        if (emailTaken) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        // 필드 업데이트
        user.updateUserInfo(requestDto.getUserLoginId(), requestDto.getUserName(), requestDto.getEmail(), passwordEncoder.encode(requestDto.getPassword()));

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userLoginId(user.getUserLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getOrgId() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null)
                .build();
    }

    // 사용자 삭제
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }

    // 사용자 전체 조회
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> UserResponseDto.builder()
                        .userId(user.getUserId())
                        .userLoginId(user.getUserLoginId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .organizationId(user.getOrganization() != null ? user.getOrganization().getOrgId() : null)
                        .departmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null)
                        .build());
    }

}
