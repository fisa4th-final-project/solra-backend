package com.fisa.solra.domain.service.service;

import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final KubernetesClient k8sClient;

    // 네임스페이스 내 서비스 전체 조회
    public List<ServiceResponseDto> getServices(String namespace) {
        // 네임스페이스 존재 여부 확인
        Namespace ns = k8sClient.namespaces().withName(namespace).get();
        if (ns == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);

        // 서비스 리스트 반환
        return k8sClient.services().inNamespace(namespace).list().getItems().stream()
                .map(ServiceResponseDto::from)
                .collect(Collectors.toList());
    }
}