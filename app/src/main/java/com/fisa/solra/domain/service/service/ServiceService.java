package com.fisa.solra.domain.service.service;


import io.fabric8.kubernetes.api.model.*;
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
import java.util.Objects;


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

    // ✅ 서비스 생성
    public ServiceResponseDto createService(String namespace, ServiceRequestDto dto) {
        // 이름 중복 검사
        if (k8sClient.services().inNamespace(namespace).withName(dto.getName()).get() != null)
            throw new BusinessException(ErrorCode.DUPLICATED_SERVICE_NAME);

        // Service 리소스 구성
        var service = new ServiceBuilder()
                .withNewMetadata().withName(dto.getName()).endMetadata()
                .withNewSpec()
                .withSelector(dto.getSelector())
                .withType(dto.getType())
                .withPorts(dto.getPorts().stream()
                        .map(p -> new ServicePortBuilder()
                                .withPort(p.getPort())
                                .withTargetPort(new IntOrString(p.getTargetPort()))
                                .withProtocol(p.getProtocol())
                                .build())
                        .collect(Collectors.toList()))
                .endSpec()
                .build();

        // 생성 요청 실행
        var created = k8sClient.services().inNamespace(namespace).resource(service).create();
        if (created == null) throw new BusinessException(ErrorCode.SERVICE_CREATION_FAILED);

        // DTO 반환
        return ServiceResponseDto.from(created);
    }

    // ✅ 서비스 수정
    public ServiceResponseDto updateService(String namespace, String name, ServiceRequestDto dto) {
        // 대상 서비스 조회

        // 사용 중인 nodePort 목록 수집
        List<Integer> usedNodePorts = k8sClient.services().inNamespace(namespace).list().getItems().stream()
                .flatMap(svc -> svc.getSpec().getPorts().stream())
                .map(ServicePort::getNodePort)
                .filter(Objects::nonNull)
                .toList();
        var svc = k8sClient.services().inNamespace(namespace).withName(name).get();
        if (svc == null) throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);

        // nodePort 중복 검사
        boolean hasConflict = dto.getPorts().stream()
                .map(p -> p.getNodePort())
                .filter(p -> p != null)
                .anyMatch(usedNodePorts::contains);
        if (hasConflict) throw new BusinessException(ErrorCode.POD_NODEPORT_CONFLICT);

        // 기존 정보와 비교하여 모두 동일하면 예외 발생
        boolean isSamePorts = svc.getSpec().getPorts().equals(dto.getPorts().stream()
                .map(p -> new ServicePortBuilder()
                        .withPort(p.getPort())
                        .withTargetPort(new IntOrString(p.getTargetPort()))
                        .withProtocol(p.getProtocol())
                        .withNodePort(p.getNodePort()) // NodePort 비교 추가
                        .build())
                .collect(Collectors.toList()));

        boolean isSameSelector = svc.getSpec().getSelector().equals(dto.getSelector());
        boolean isSameType = svc.getSpec().getType().equals(dto.getType());

        if (isSamePorts && isSameSelector && isSameType) {
            throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED); // 변경 사항 없음
        }

        // edit() 사용하여 리소스 수정
        var updated = k8sClient.services().inNamespace(namespace).withName(name).edit(s -> {
            s.getSpec().setPorts(dto.getPorts().stream()
                    .map(p -> new ServicePortBuilder()
                            .withPort(p.getPort())
                            .withTargetPort(new IntOrString(p.getTargetPort()))
                            .withProtocol(p.getProtocol())
                            .withNodePort(p.getNodePort()) // NodePort 설정 반영
                            .build())
                    .collect(Collectors.toList()));

            s.getSpec().setSelector(dto.getSelector());

            if (dto.getType() != null) {
                s.getSpec().setType(dto.getType());
            }

            return s;
        });

        if (updated == null) throw new BusinessException(ErrorCode.SERVICE_UPDATE_FAILED);

        // 수정 결과 반환
        return ServiceResponseDto.from(updated);
    }
}