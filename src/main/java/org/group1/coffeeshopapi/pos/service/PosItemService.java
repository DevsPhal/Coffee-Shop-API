package org.group1.coffeeshopapi.pos.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.pos.dto.request.PosItemRequest;
import org.group1.coffeeshopapi.pos.dto.response.PosItemResponse;

public interface PosItemService {
    PosItemResponse create(PosItemRequest request);
    PosItemResponse getById(UUID id);
    PosItemResponse update(UUID id, PosItemRequest request);
    void delete(UUID id);
    List<PosItemResponse> getAll();
    List<PosItemResponse> getAllActive();
}
