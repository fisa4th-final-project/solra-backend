package com.fisa.solra.domain.organization.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponseDto {
    private Long orgId;
    private String orgName;
}
