package org.group1.coffeeshopapi.setting.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.setting.dto.request.SettingRequest;
import org.group1.coffeeshopapi.setting.dto.response.SettingResponse;
import org.group1.coffeeshopapi.setting.service.SettingService;
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
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SettingResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<SettingResponse>>builder().status(HttpStatus.OK.value()).message("Settings retrieved successfully").data(settingService.getAll()).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SettingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<SettingResponse>builder().status(HttpStatus.OK.value()).message("Setting retrieved successfully").data(settingService.getById(id)).timeStamp(LocalDateTime.now()).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SettingResponse>> create(@RequestBody SettingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<SettingResponse>builder().status(HttpStatus.CREATED.value()).message("Setting created successfully").data(settingService.create(request)).timeStamp(LocalDateTime.now()).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SettingResponse>> update(@PathVariable Long id, @RequestBody SettingRequest request) {
        return ResponseEntity.ok(ApiResponse.<SettingResponse>builder().status(HttpStatus.OK.value()).message("Setting updated successfully").data(settingService.update(id, request)).timeStamp(LocalDateTime.now()).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        settingService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.<Void>builder().status(HttpStatus.NO_CONTENT.value()).message("Setting deleted successfully").timeStamp(LocalDateTime.now()).build());
    }
}