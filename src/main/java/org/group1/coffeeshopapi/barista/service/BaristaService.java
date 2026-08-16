package org.group1.coffeeshopapi.barista.service;

import java.util.UUID;

import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.springframework.data.domain.Pageable;

public interface BaristaService {
    PaginatedResponse<BaristaResponse> getAllBaristas(Pageable pageable);
    BaristaResponse getBaristaById(UUID id);
    BaristaResponse createBarista(BaristaRequest request);
    BaristaResponse updateBarista(UUID id, BaristaRequest request);
    void deleteBarista(UUID id);
}
