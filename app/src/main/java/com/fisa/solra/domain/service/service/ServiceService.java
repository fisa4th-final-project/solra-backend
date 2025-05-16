package com.fisa.solra.domain.service.service;


import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.client.KubernetesClient;

// 🔽 DTO/예외/로직 관련
import com.fisa.solra.domain.service.dto.ServiceRequestDto;
import com.fisa.solra.domain.service.dto.ServiceResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;

// 🔽 Spring 서비스 어노테이션
import org.springframework.stereotype.Service; // ✅ Spring의 @Service
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ServiceService {

    private final KubernetesClient k8sClient;

    // Fabric8 Service 타입 (클래스 전체에서 사용할 수 있도록 별칭 변수 선언)
    private final Class<io.fabric8.kubernetes.api.model.Service> k8sServiceType = io.fabric8.kubernetes.api.model.Service.class;

    // ✅ 네임스페이스 내 서비스 전체 조회
    public List<ServiceResponseDto> getServices(String namespace) {
        // 네임스페이스 존재 여부 확인
        Namespace ns = k8sClient.namespaces().withName(namespace).get();
        if (ns == null) throw new BusinessException(ErrorCode.NAMESPACE_NOT_FOUND);

        // 전체 서비스 리스트 반환
        return k8sClient.services().inNamespace(namespace).list().getItems().stream()
                .map(ServiceResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 서비스 조회
    public ServiceResponseDto getService(String namespace, String name) {
        // 대상 서비스 조회
        var svc = k8sClient.services().inNamespace(namespace).withName(name).get();
        if (svc == null) throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);

        // DTO 변환 후 반환
        return ServiceResponseDto.from(svc);
    }
}