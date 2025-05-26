package com.fisa.solra.domain.user.repository;

import com.fisa.solra.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 ID로 User 정보 가져오기
    Optional<User> findByUserLoginId(String userLoginId);

    // 로그인 ID로 사용자 존재 여부 확인
    boolean existsByUserLoginId(String userLoginId);
    // 사용자 이메일 중복 검사
    boolean existsByEmailAndUserIdNot(String email, Long userId);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN u.organization o " +
            "LEFT JOIN u.department d " +
            "WHERE (:orgName IS NULL OR o.orgName LIKE %:orgName%) " +
            "AND (:deptName IS NULL OR d.deptName LIKE %:deptName%)")
    Page<User> findByOrgNameAndDeptName(@Param("orgName") String orgName,
                                        @Param("deptName") String deptName,
                                        Pageable pageable);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN u.organization o " +
            "LEFT JOIN u.department d " +
            "WHERE (:userName IS NULL OR u.userName LIKE %:userName%) " +
            "AND (:email IS NULL OR u.email LIKE %:email%) " +
            "AND (:orgName IS NULL OR o.orgName LIKE %:orgName%) " +
            "AND (:deptName IS NULL OR d.deptName LIKE %:deptName%)")
    List<User> searchByConditions(@Param("userName") String userName,
                                  @Param("email") String email,
                                  @Param("orgName") String orgName,
                                  @Param("deptName") String deptName);

}
