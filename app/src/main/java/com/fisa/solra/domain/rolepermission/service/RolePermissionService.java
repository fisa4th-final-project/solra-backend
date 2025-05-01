package com.fisa.solra.domain.rolepermission.service;

import com.fisa.solra.domain.rolepermission.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

}
