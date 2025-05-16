package com.fisa.solra.domain.pod.service;

import com.fisa.solra.domain.pod.dto.PodResponseDto;
import com.fisa.solra.global.exception.BusinessException;
import com.fisa.solra.global.exception.ErrorCode;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PodService {

    private final KubernetesClient k8sClient;

    // ✅ 파드 전체 조회
    public List<PodResponseDto> getPods(String namespace) {
        return k8sClient.pods().inNamespace(namespace).list().getItems().stream()
                .map(PodResponseDto::from)
                .collect(Collectors.toList());
    }

    // ✅ 단일 파드 조회
    public PodResponseDto getPod(String namespace, String name) {
        Pod pod = k8sClient.pods().inNamespace(namespace).withName(name).get();
        if (pod == null) throw new BusinessException(ErrorCode.POD_NOT_FOUND);
        return PodResponseDto.from(pod);
    }
}