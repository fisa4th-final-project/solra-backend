package com.fisa.solra.domain.user.repository;

import com.fisa.solra.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 ID로 User 정보 가져오기
    Optional<User> findByUserLoginId(String userLoginId);

    // 로그인 ID로 사용자 존재 여부 확인
    boolean existsByUserLoginId(String userLoginId);
    // 사용자 이메일 중복 검사
    boolean existsByEmailAndUserIdNot(String email, Long userId);
}
