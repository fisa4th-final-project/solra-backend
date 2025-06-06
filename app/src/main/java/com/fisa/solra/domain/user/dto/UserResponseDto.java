package com.fisa.solra.domain.user.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long userId;
    private String userName;
    private String userLoginId;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Long organizationId;
    private String organizationName;
    private List<String> permNames;
}
