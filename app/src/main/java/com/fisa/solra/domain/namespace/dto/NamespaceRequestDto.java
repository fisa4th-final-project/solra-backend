package com.fisa.solra.domain.namespace.dto;


import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class NamespaceRequestDto {
    private String name;
    private Map<String, String> labels;
    private Map<String, String> annotations;
}
