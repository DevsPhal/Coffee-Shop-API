package org.group1.coffeeshopapi.banner.service;

import org.group1.coffeeshopapi.banner.dto.request.CreateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.request.UpdateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.response.BannerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BannerService {
    BannerResponse create(CreateBannerRequest request);
    BannerResponse getById(UUID id);
    Page<BannerResponse> list(Pageable pageable);
    List<BannerResponse> listActive();
    BannerResponse update(UUID id, UpdateBannerRequest request);
    void delete(UUID id);

    BannerResponse uploadImage(UUID id, MultipartFile file);
    BannerResponse removeImage(UUID id);
}
