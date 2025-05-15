package com.fisa.solra.domain.namespace.service;

import com.fisa.solra.domain.namespace.dto.NamespaceRequestDto;
import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NamespaceService {
    private final KubernetesClient k8sClient;

    // ✅ 전체 네임스페이스 조회
    public List<NamespaceResponseDto> getNamespaces() {
        return k8sClient.namespaces().list().getItems().stream()
                .map(NamespaceResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 네임스페이스 상세 조회
    public NamespaceResponseDto getNamespace(String name) {
        Namespace ns = k8sClient.namespaces().withName(name).get();
        if (ns == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        return NamespaceResponseDto.from(ns);
    }
    // ✅ 네임스페이스 생성
    public NamespaceResponseDto createNamespace(NamespaceRequestDto dto) {
        boolean exists = k8sClient.namespaces().withName(dto.getName()).get() != null;
        if (exists) throw new BusinessException(ErrorCode.DUPLICATED_NAMESPACE_NAME);

        Namespace created = k8sClient.namespaces().resource(new NamespaceBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .addToLabels(dto.getLabels())
                .addToAnnotations(dto.getAnnotations())
                .endMetadata()
                .build()).create();

        if (created == null) throw new BusinessException(ErrorCode.NAMESPACE_CREATION_FAILED);
        return NamespaceResponseDto.from(created);
    }
}
