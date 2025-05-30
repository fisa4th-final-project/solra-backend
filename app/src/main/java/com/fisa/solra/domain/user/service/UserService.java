package com.fisa.solra.domain.user.service;

import com.fisa.solra.domain.department.entity.Department;
import com.fisa.solra.domain.department.repository.DepartmentRepository;
import com.fisa.solra.domain.organization.entity.Organization;
import com.fisa.solra.domain.organization.repository.OrganizationRepository;
import com.fisa.solra.domain.permission.service.PermissionService;
import com.fisa.solra.domain.user.dto.UserCreateRequestDto;
import com.fisa.solra.domain.user.dto.UserLoginInfo;
import com.fisa.solra.domain.user.dto.UserResponseDto;
import com.fisa.solra.domain.user.dto.UserUpdateRequestDto;
import com.fisa.solra.domain.user.entity.User;
import com.fisa.solra.domain.user.repository.UserRepository;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import com.fisa.solra.global.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

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

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userLoginId(user.getUserLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getOrgId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getOrgName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null)
                .departmentName(user.getOrganization() != null ? user.getDepartment().getDeptName() : null)
                .build();
    }

    // 사용자 수정
    @Transactional
    public UserResponseDto updateUser(Long targetUserId, UserUpdateRequestDto requestDto) {
        Long currentOrgId = SecurityUtil.getOrgId();

        // 1) 권한 검사 (ROOT 우회 포함됨)
        permissionService.checkPermission("USER_UPDATE");

        // 2) 대상 사용자 조회
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3) 조직 일치 여부 확인 (ROOT는 생략)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(user.getOrganization().getOrgId(), currentOrgId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4) 로그인 ID 중복 검사 (자기 자신 제외)
        if (requestDto.getUserLoginId() != null &&
                !requestDto.getUserLoginId().equals(user.getUserLoginId()) &&
                userRepository.existsByUserLoginId(requestDto.getUserLoginId())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATED);
        }

        // 5) 이메일 중복 검사 (자기 자신 제외)
        if (userRepository.existsByEmailAndUserIdNot(requestDto.getEmail(), targetUserId)) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        // 6) 정보 업데이트
        user.updateUserInfo(
                requestDto.getUserLoginId(),
                requestDto.getUserName(),
                requestDto.getEmail(),
                passwordEncoder.encode(requestDto.getPassword())
        );

        // 7) 응답 생성
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
    public void deleteUser(Long targetUserId) {
        Long currentOrgId = SecurityUtil.getOrgId();

        // 1) 권한 검사 (ROOT 포함 처리됨)
        permissionService.checkPermission("USER_DELETE");

        // 2) 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3) 조직 일치 여부 확인 (ROOT는 우회)
        if (!SecurityUtil.hasRole("ROOT") &&
                !Objects.equals(targetUser.getOrganization().getOrgId(), currentOrgId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4) 삭제 처리
        userRepository.delete(targetUser);
    }

    // 사용자 전체 조회
    public Page<UserResponseDto> getAllUsers(Pageable pageable, String orgName, String deptName) {
        Page<User> userPage;

        if ((orgName == null || orgName.isBlank()) && (deptName == null || deptName.isBlank())) {
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.findByOrgNameAndDeptName(orgName, deptName, pageable);
        }

        return userPage.map(user -> UserResponseDto.builder()
                .userId(user.getUserId())
                .userLoginId(user.getUserLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getOrgId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getOrgName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getDeptName() : null)
                .build());
    }

    // 사용자 이름, 조직, 부서, 이메일 검색
    public List<UserResponseDto> searchUsers(String userName, String email, String orgName, String deptName) {
        List<User> users = userRepository.searchByConditions(userName, email, orgName, deptName);

        return users.stream()
                .map(user -> UserResponseDto.builder()
                        .userId(user.getUserId())
                        .userLoginId(user.getUserLoginId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .organizationId(user.getOrganization() != null ? user.getOrganization().getOrgId() : null)
                        .organizationName(user.getOrganization() != null ? user.getOrganization().getOrgName() : null)
                        .departmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null)
                        .departmentName(user.getDepartment() != null ? user.getDepartment().getDeptName() : null)
                        .build())
                .toList();
    }



}
