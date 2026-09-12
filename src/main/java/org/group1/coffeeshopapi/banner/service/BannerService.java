package org.group1.coffeeshopapi.banner.service;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.banner.dto.request.CreateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.request.UpdateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.response.BannerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// actorAdmin is null when the Super Admin is the one acting — see CurrentActor.adminRef().
public interface BannerService {
    BannerResponse create(CreateBannerRequest request, Admin actorAdmin);
    BannerResponse getById(UUID id);
    Page<BannerResponse> list(Pageable pageable);
    List<BannerResponse> listActive();
    BannerResponse update(UUID id, UpdateBannerRequest request, Admin actorAdmin);
    void delete(UUID id);

    BannerResponse uploadImage(UUID id, MultipartFile file, Admin actorAdmin);
    BannerResponse removeImage(UUID id, Admin actorAdmin);
}
