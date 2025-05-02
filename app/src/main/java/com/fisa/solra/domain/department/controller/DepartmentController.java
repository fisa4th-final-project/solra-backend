package com.fisa.solra.domain.department.controller;

import com.fisa.solra.domain.department.dto.DepartmentRequestDto;
import com.fisa.solra.domain.department.dto.DepartmentResponseDto;
import com.fisa.solra.domain.department.service.DepartmentService;
import com.fisa.solra.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    // 전체 부서 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponseDto>>> getDepartments() {
        List<DepartmentResponseDto> list = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success(list, "부서 목록 조회 성공"));
    }

    // 단일 부서 조회
    @GetMapping("/{deptId}")
    public ResponseEntity<ApiResponse<DepartmentResponseDto>> getDepartment(
            @PathVariable Long deptId) {
        DepartmentResponseDto dto = departmentService.getDepartmentById(deptId);
        return ResponseEntity.ok(ApiResponse.success(dto, "부서 조회 성공"));
    }

    // 부서 생성
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponseDto>> createDepartment(
            @RequestBody DepartmentRequestDto requestDto) {
        DepartmentResponseDto dto = departmentService.createDepartment(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "부서 생성 성공"));
    }

    // 부서명 수정
    @PatchMapping("/{deptId}")
    public ResponseEntity<ApiResponse<DepartmentResponseDto>> updateDepartment(
            @PathVariable Long deptId,
            @RequestBody DepartmentRequestDto requestDto) {
        DepartmentResponseDto dto = departmentService.updateDepartmentName(deptId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(dto, "부서명 수정 성공"));
    }

    // 부서 삭제
    @DeleteMapping("/{deptId}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(
            @PathVariable Long deptId) {
        departmentService.deleteDepartment(deptId);
        return ResponseEntity.ok(ApiResponse.success(null, "부서 삭제 성공"));
    }
}
