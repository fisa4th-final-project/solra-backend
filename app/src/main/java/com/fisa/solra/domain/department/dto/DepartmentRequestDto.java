package com.fisa.solra.domain.department.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequestDto {
    private Long organizationId;
    private String deptName;
}