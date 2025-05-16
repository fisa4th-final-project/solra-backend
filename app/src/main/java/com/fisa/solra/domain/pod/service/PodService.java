package com.fisa.solra.domain.pod.service;

import com.fisa.solra.domain.pod.dto.PodResponseDto;
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
}