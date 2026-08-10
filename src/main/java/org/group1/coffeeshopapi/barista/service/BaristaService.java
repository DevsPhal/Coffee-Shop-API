package org.group1.coffeeshopapi.barista.service;

import java.util.List;

import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;

public interface BaristaService {
    List<BaristaResponse> getAllBaristas();
    BaristaResponse getBaristaById(Long id);
    BaristaResponse createBarista(BaristaRequest request);
    BaristaResponse updateBarista(Long id, BaristaRequest request);
    void deleteBarista(Long id);
}
