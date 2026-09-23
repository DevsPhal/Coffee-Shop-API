package org.group1.coffeeshopapi.extra.service;

import org.group1.coffeeshopapi.extra.dto.request.CreateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateExtraRequest;
import org.group1.coffeeshopapi.extra.dto.response.ExtraResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ExtraService {
    ExtraResponse create(CreateExtraRequest request);
    List<ExtraResponse> list();
    ExtraResponse update(UUID id, UpdateExtraRequest request);
    void delete(UUID id);
    ExtraResponse uploadImage(UUID id, MultipartFile file);
    ExtraResponse removeImage(UUID id);
}
