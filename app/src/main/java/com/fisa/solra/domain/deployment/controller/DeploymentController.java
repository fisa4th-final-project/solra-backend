package com.fisa.solra.domain.deployment.controller;

import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.domain.deployment.service.DeploymentService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    // ✅ 디플로이먼트 리스트 조회
    @GetMapping("/deployments")
    public ResponseEntity<ApiResponse<List<DeploymentResponseDto>>> getDeployments(
            @RequestParam(name = "namespaces") String namespace) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        deploymentService.getDeployments(namespace),
                        "디플로이먼트 리스트 조회에 성공했습니다."
                )
        );
    }

    // ✅ 디플로이먼트 상세 조회
    @GetMapping("/namespaces/{namespace}/deployments/{name}")
    public ResponseEntity<ApiResponse<DeploymentResponseDto>> getDeployment(@PathVariable String namespace, @PathVariable String name) {
        return ResponseEntity.ok(
                ApiResponse.success(deploymentService.getDeployment(namespace, name), "디플로이먼트 상세 조회에 성공했습니다.")
        );
    }
}
