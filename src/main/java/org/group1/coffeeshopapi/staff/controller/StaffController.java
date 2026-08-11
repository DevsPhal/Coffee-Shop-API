package org.group1.coffeeshopapi.staff.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;
import org.group1.coffeeshopapi.staff.service.StaffService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StaffResponse>>> getAllStaff(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        PageResponse<StaffResponse> staff = staffService.getAllStaff(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<StaffResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully")
                .data(staff)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaffById(@PathVariable UUID id) {
        StaffResponse staff = staffService.getStaffById(id);
        return ResponseEntity.ok(ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff retrieved successfully")
                .data(staff)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(@RequestBody StaffRequest request) {
        StaffResponse staff = staffService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StaffResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Staff created successfully")
                        .data(staff)
                        .timeStamp(LocalDateTime.now())
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(@PathVariable UUID id, @RequestBody StaffRequest request) {
        StaffResponse staff = staffService.updateStaff(id, request);
        return ResponseEntity.ok(ApiResponse.<StaffResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff updated successfully")
                .data(staff)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable UUID id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Staff deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}