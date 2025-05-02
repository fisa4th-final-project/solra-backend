package com.fisa.solra.domain.userpermission.service;

import com.fisa.solra.domain.userpermission.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserPermissionRepository userRoleRepository;

}
