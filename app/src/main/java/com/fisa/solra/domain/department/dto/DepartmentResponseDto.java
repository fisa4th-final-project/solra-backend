package com.fisa.solra.domain.department.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponseDto {
    private Long orgId;
    private String orgName;
    private Long deptId;
    private String deptName;
}
