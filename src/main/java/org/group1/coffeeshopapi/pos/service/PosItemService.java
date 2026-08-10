package org.group1.coffeeshopapi.pos.service;

import java.util.List;

import org.group1.coffeeshopapi.pos.dto.request.PosItemRequest;
import org.group1.coffeeshopapi.pos.dto.response.PosItemResponse;

public interface PosItemService {
    PosItemResponse create(PosItemRequest request);
    PosItemResponse getById(Long id);
    PosItemResponse update(Long id, PosItemRequest request);
    void delete(Long id);
    List<PosItemResponse> getAll();
    List<PosItemResponse> getAllActive();
}
