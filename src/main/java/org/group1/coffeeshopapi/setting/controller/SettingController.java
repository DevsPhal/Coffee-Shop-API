package org.group1.coffeeshopapi.setting.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.setting.dto.request.SettingRequest;
import org.group1.coffeeshopapi.setting.dto.response.SettingResponse;
import org.group1.coffeeshopapi.setting.service.SettingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public List<SettingResponse> getAll() {
        return settingService.getAll();
    }

    @GetMapping("/{id}")
    public SettingResponse getById(@PathVariable UUID id) {
        return settingService.getById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SettingResponse create(@RequestBody SettingRequest request) {
        return settingService.create(request);
    }

    @PutMapping("/{id}")
    public SettingResponse update(@PathVariable UUID id, @RequestBody SettingRequest request) {
        return settingService.update(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        settingService.delete(id);
    }
}
