package org.group1.coffeeshopapi.extra.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.extra.dto.request.CreateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.response.ExtraResponse;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.mapper.ExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.service.ExtraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtraServiceImpl implements ExtraService {

    private final ExtraRepository extraRepository;
    private final ExtraMapper extraMapper;

    @Override
    @Transactional
    public ExtraResponse create(CreateExtraRequest request) {
        if (extraRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("An extra named '" + request.name() + "' already exists");
        }
        Extra extra = new Extra();
        extra.setName(request.name());
        extra.setPrice(request.price());
        return extraMapper.toResponse(extraRepository.save(extra));
    }

    @Override
    public List<ExtraResponse> list() {
        return extraRepository.findAll().stream()
                .sorted(Comparator.comparing(Extra::getName, String.CASE_INSENSITIVE_ORDER))
                .map(extraMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExtraResponse update(UUID id, UpdateExtraRequest request) {
        Extra extra = findById(id);

        if (request.name() != null) {
            if (!request.name().equalsIgnoreCase(extra.getName())
                    && extraRepository.existsByNameIgnoreCase(request.name())) {
                throw new DuplicateResourceException("An extra named '" + request.name() + "' already exists");
            }
            extra.setName(request.name());
        }
        if (request.price() != null) {
            extra.setPrice(request.price());
        }
        if (request.status() != null) {
            extra.setStatus(request.status());
        }

        return extraMapper.toResponse(extraRepository.save(extra));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        // Deleting an Extra still referenced by a past order's OrderItemExtra (or attached to a
        // product via ProductExtra) hits a FK constraint and is rejected — see
        // GlobalExceptionHandler's DataIntegrityViolationException handler — same as
        // ProductVariant's delete.
        extraRepository.delete(findById(id));
    }

    private Extra findById(UUID id) {
        return extraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Extra not found"));
    }
}
