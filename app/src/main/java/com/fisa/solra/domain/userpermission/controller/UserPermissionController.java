package com.fisa.solra.domain.userpermission.controller;

import com.fisa.solra.domain.userpermission.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;

}
