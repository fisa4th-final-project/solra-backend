package com.fisa.solra.domain.deployment.controller;

import com.fisa.solra.domain.deployment.dto.DeploymentCreateRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentCreateResponseDto;
import com.fisa.solra.domain.deployment.dto.DeploymentRequestDto;
import com.fisa.solra.domain.deployment.dto.DeploymentResponseDto;
import com.fisa.solra.domain.deployment.service.DeploymentService;
import com.fisa.solra.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/namespaces/{namespace}/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    // ✅ 디플로이먼트 리스트 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeploymentResponseDto>>> list(
            @PathVariable String namespace) {
        List<DeploymentResponseDto> dtos =
                deploymentService.getDeployments(namespace);
        return ResponseEntity.ok(
                ApiResponse.success(dtos, "디플로이먼트 리스트 조회에 성공했습니다."));
    }

    // ✅ 디플로이먼트 상세 조회
    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<DeploymentResponseDto>> get(
            @PathVariable String namespace,
            @PathVariable String name) {
        DeploymentResponseDto dto =
                deploymentService.getDeployment(namespace, name);
        return ResponseEntity.ok(
                ApiResponse.success(dto, "디플로이먼트 상세 조회에 성공했습니다."));
    }

    // ✅ 디플로이먼트 생성
    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentCreateResponseDto>> create(
            @PathVariable String namespace,
            @Valid @RequestBody DeploymentCreateRequestDto req) {

        DeploymentCreateResponseDto created =
                deploymentService.createDeployment(namespace, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        created,
                        "디플로이먼트 생성에 성공했습니다."
                ));
    }

}
