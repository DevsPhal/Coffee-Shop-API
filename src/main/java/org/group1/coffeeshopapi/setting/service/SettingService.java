package org.group1.coffeeshopapi.setting.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.setting.dto.request.SettingRequest;
import org.group1.coffeeshopapi.setting.dto.response.SettingResponse;

public interface SettingService {
    List<SettingResponse> getAll();
    SettingResponse getById(UUID id);
    SettingResponse create(SettingRequest request);
    SettingResponse update(UUID id, SettingRequest request);
    void delete(UUID id);
}
