package org.group1.coffeeshopapi.setting.service;

import java.util.List;

import org.group1.coffeeshopapi.setting.dto.request.SettingRequest;
import org.group1.coffeeshopapi.setting.dto.response.SettingResponse;

public interface SettingService {
    List<SettingResponse> getAll();
    SettingResponse getById(Long id);
    SettingResponse create(SettingRequest request);
    SettingResponse update(Long id, SettingRequest request);
    void delete(Long id);
}
