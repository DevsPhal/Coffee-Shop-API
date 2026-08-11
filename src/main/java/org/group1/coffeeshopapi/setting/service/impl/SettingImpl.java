package org.group1.coffeeshopapi.setting.service.impl;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.setting.dto.request.SettingRequest;
import org.group1.coffeeshopapi.setting.dto.response.SettingResponse;
import org.group1.coffeeshopapi.setting.entity.Setting;
import org.group1.coffeeshopapi.setting.mapper.SettingMapper;
import org.group1.coffeeshopapi.setting.repository.SettingRepository;
import org.group1.coffeeshopapi.setting.service.SettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingImpl implements SettingService {

    private final SettingRepository repository;
    private final SettingMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SettingResponse getById(UUID id) {
        Setting s = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + id));
        return mapper.toResponse(s);
    }

    @Override
    public SettingResponse create(SettingRequest request) {
        if (repository.findByKey(request.getKey()).isPresent()) {
            throw new DuplicateResourceException("Setting with key " + request.getKey() + " already exists");
        }
        Setting s = mapper.toEntity(request);
        Setting saved = repository.save(s);
        return mapper.toResponse(saved);
    }

    @Override
    public SettingResponse update(UUID id, SettingRequest request) {
        Setting s = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + id));
        repository.findByKey(request.getKey())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Setting with key " + request.getKey() + " already exists");
                });
        s.setKey(request.getKey());
        s.setValue(request.getValue());
        return mapper.toResponse(repository.save(s));
    }

    @Override
    public void delete(UUID id) {
        Setting s = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + id));
        repository.delete(s);
    }
}
