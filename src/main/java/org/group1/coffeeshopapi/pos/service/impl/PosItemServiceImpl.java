package org.group1.coffeeshopapi.pos.service.impl;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.pos.dto.request.PosItemRequest;
import org.group1.coffeeshopapi.pos.dto.response.PosItemResponse;
import org.group1.coffeeshopapi.pos.entity.PosItem;
import org.group1.coffeeshopapi.pos.mapper.PosItemMapper;
import org.group1.coffeeshopapi.pos.repository.PosItemRepository;
import org.group1.coffeeshopapi.pos.service.PosItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class PosItemServiceImpl implements PosItemService {

    private final PosItemRepository repository;
    private final PosItemMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PosItemResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosItemResponse> getAllActive() {
        return repository.findAllByActiveTrue().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosItemResponse getById(UUID id) {
        PosItem item = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("POS item not found: " + id));
        return mapper.toResponse(item);
    }

    @Override
    public PosItemResponse create(PosItemRequest request) {
        PosItem item = mapper.toEntity(request);
        PosItem saved = repository.save(item);
        return mapper.toResponse(saved);
    }

    @Override
    public PosItemResponse update(UUID id, PosItemRequest request) {
        PosItem item = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("POS item not found: " + id));
        mapper.updateEntity(request, item);
        return mapper.toResponse(repository.save(item));
    }

    @Override
    public void delete(UUID id) {
        PosItem item = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("POS item not found: " + id));
        repository.delete(item);
    }
}
