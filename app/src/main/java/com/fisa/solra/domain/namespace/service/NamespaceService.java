package com.fisa.solra.domain.namespace.service;

import com.fisa.solra.domain.namespace.dto.NamespaceRequestDto;
import com.fisa.solra.domain.namespace.dto.NamespaceResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
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

    // ✅ 네임스페이스 수정 (라벨/어노테이션만 허용)
    public NamespaceResponseDto updateNamespace(String name, NamespaceRequestDto dto) {
        Namespace ns = k8sClient.namespaces().withName(name).get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }

        var meta       = ns.getMetadata();
        var oldLabels  = meta.getLabels();
        var oldAnnos   = meta.getAnnotations();
        var newLabels  = dto.getLabels();
        var newAnnos   = dto.getAnnotations();

        // → 요청한 키만 꺼내서 비교
        boolean labelsUnchanged = newLabels.entrySet().stream()
                .allMatch(e -> e.getValue().equals(oldLabels.get(e.getKey())));
        boolean annosUnchanged  = newAnnos.entrySet().stream()
                .allMatch(e -> e.getValue().equals(oldAnnos.get(e.getKey())));

        if (labelsUnchanged && annosUnchanged) {
            throw new BusinessException(ErrorCode.NAMESPACE_UPDATE_NO_CHANGE);
        }

        // edit() 로 실제 patch
        Namespace updated = k8sClient.namespaces()
                .withName(name)
                .edit(n -> new NamespaceBuilder(n)
                        .editMetadata()
                        .withLabels(newLabels)
                        .withAnnotations(newAnnos)
                        .endMetadata()
                        .build()
                );
        if (updated == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_UPDATE_FAILED);
        }
        return NamespaceResponseDto.from(updated);
    }

    // ✅ 네임스페이스 삭제
    public void deleteNamespace(String name) {
        // 1) 네임스페이스 존재 여부 확인
        Namespace ns = k8sClient.namespaces().withName(name).get();
        if (ns == null) {
            throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);
        }

        // 2) List<StatusDetails> 로 받도록 변경
        List<StatusDetails> statusDetails =
                k8sClient.namespaces()
                        .resource(ns)
                        .delete();

        // 3) 정상 삭제 시 한 개 이상의 StatusDetails 가 반환되므로, 비어 있으면 실패로 간주
        if (statusDetails == null || statusDetails.isEmpty()) {
            throw new BusinessException(ErrorCode.NAMESPACE_DELETION_FAILED);
        }
    }
}
