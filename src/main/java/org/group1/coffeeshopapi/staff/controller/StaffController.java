package org.group1.coffeeshopapi.staff.controller;

import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;
import org.group1.coffeeshopapi.staff.service.StaffService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public PaginatedResponse<StaffResponse> getAllStaff(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return staffService.getAllStaff(pageable);
    }

    @GetMapping("/{id}")
    public StaffResponse getStaffById(@PathVariable UUID id) {
        return staffService.getStaffById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public StaffResponse createStaff(@RequestBody StaffRequest request) {
        return staffService.createStaff(request);
    }

    @PutMapping("/{id}")
    public StaffResponse updateStaff(@PathVariable UUID id, @RequestBody StaffRequest request) {
        return staffService.updateStaff(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteStaff(@PathVariable UUID id) {
        staffService.deleteStaff(id);
    }
}
