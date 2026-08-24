package org.group1.coffeeshopapi.banner.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.banner.dto.request.CreateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.request.UpdateBannerRequest;
import org.group1.coffeeshopapi.banner.dto.response.BannerResponse;
import org.group1.coffeeshopapi.banner.entity.Banner;
import org.group1.coffeeshopapi.banner.mapper.BannerMapper;
import org.group1.coffeeshopapi.banner.repository.BannerRepository;
import org.group1.coffeeshopapi.banner.service.BannerService;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private static final String IMAGE_FOLDER = "banners";

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final FileStorageService fileStorageService;
    private final ActorLookupService actorLookupService;

    @Override
    @Transactional
    public BannerResponse create(CreateBannerRequest request, UUID actorId) {
        Banner banner = new Banner();
        banner.setTitle(request.title());
        banner.setLinkUrl(request.linkUrl());
        banner.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        banner.setAdminId(actorId);
        banner.setUpdatedByAdminId(actorId);
        return toResponse(bannerRepository.save(banner));
    }

    @Override
    public BannerResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<BannerResponse> list(Pageable pageable) {
        Page<Banner> banners = bannerRepository.findAllByOrderBySortOrderAsc(pageable);

        Set<UUID> actorIds = new HashSet<>();
        for (Banner banner : banners) {
            actorIds.add(banner.getAdminId());
            actorIds.add(banner.getUpdatedByAdminId());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        return banners.map(banner -> bannerMapper.toResponse(banner,
                actors.get(banner.getAdminId()), actors.get(banner.getUpdatedByAdminId())));
    }

    @Override
    public List<BannerResponse> listActive() {
        // Public, unauthenticated endpoint (storefront landing page) — deliberately doesn't
        // resolve staff identities here, so admin/super admin names never leak to anonymous
        // visitors. adminId/updatedByAdminId still come through as raw ids for parity with the
        // admin-facing response shape.
        return bannerRepository.findByStatusOrderBySortOrderAsc(Status.ACTIVE).stream()
                .map(banner -> bannerMapper.toResponse(banner, null, null))
                .toList();
    }

    @Override
    @Transactional
    public BannerResponse update(UUID id, UpdateBannerRequest request, UUID actorId) {
        Banner banner = findById(id);
        if (request.title() != null) {
            banner.setTitle(request.title());
        }
        if (request.linkUrl() != null) {
            banner.setLinkUrl(request.linkUrl());
        }
        if (request.sortOrder() != null) {
            banner.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            banner.setStatus(request.status());
        }
        banner.setUpdatedByAdminId(actorId);
        return toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Banner banner = findById(id);
        if (banner.getImageUrl() != null) {
            fileStorageService.delete(banner.getImageUrl());
        }
        bannerRepository.delete(banner);
    }

    @Override
    @Transactional
    public BannerResponse uploadImage(UUID id, MultipartFile file, UUID actorId) {
        Banner banner = findById(id);
        String previousImageUrl = banner.getImageUrl();

        banner.setImageUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        banner.setUpdatedByAdminId(actorId);
        banner = bannerRepository.save(banner);

        if (previousImageUrl != null) {
            fileStorageService.delete(previousImageUrl);
        }
        return toResponse(banner);
    }

    @Override
    @Transactional
    public BannerResponse removeImage(UUID id, UUID actorId) {
        Banner banner = findById(id);
        if (banner.getImageUrl() != null) {
            fileStorageService.delete(banner.getImageUrl());
            banner.setImageUrl(null);
            banner.setUpdatedByAdminId(actorId);
            banner = bannerRepository.save(banner);
        }
        return toResponse(banner);
    }

    private BannerResponse toResponse(Banner banner) {
        return bannerMapper.toResponse(banner,
                actorLookupService.resolve(banner.getAdminId()),
                actorLookupService.resolve(banner.getUpdatedByAdminId()));
    }

    private Banner findById(UUID id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
    }
}
