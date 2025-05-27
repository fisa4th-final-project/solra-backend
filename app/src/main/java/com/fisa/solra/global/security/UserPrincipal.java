package com.fisa.solra.global.security;

import org.apache.catalina.security.SecurityUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final Long orgId;
    private final Long deptId;
    private final List<String> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId, Long orgId, Long deptId,
                         List<String> roles,
                         Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.orgId = orgId;
        this.deptId = deptId;
        this.roles = roles;
        this.authorities = authorities;
    }

    public Long getUserId() { return userId; }
    public Long getOrgId() { return orgId; }
    public Long getDeptId() { return deptId; }
    public List<String> getRoles() { return roles; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 사용하지 않지만 인터페이스상 구현 필요
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return String.valueOf(userId); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
