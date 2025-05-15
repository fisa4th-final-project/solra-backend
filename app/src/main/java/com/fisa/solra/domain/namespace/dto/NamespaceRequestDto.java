package com.fisa.solra.domain.namespace.dto;


import lombok.Getter;

import java.util.Map;

@Getter
public class NamespaceRequestDto {
    private String name;
    private Map<String, String> labels;
    private Map<String, String> annotations;
}
