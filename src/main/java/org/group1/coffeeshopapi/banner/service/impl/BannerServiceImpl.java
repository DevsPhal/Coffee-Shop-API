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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private static final String IMAGE_FOLDER = "banners";

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public BannerResponse create(CreateBannerRequest request) {
        Banner banner = new Banner();
        banner.setTitle(request.title());
        banner.setLinkUrl(request.linkUrl());
        banner.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Override
    public BannerResponse getById(UUID id) {
        return bannerMapper.toResponse(findById(id));
    }

    @Override
    public Page<BannerResponse> list(Pageable pageable) {
        return bannerRepository.findAllByOrderBySortOrderAsc(pageable).map(bannerMapper::toResponse);
    }

    @Override
    public List<BannerResponse> listActive() {
        return bannerRepository.findByStatusOrderBySortOrderAsc(Status.ACTIVE).stream()
                .map(bannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BannerResponse update(UUID id, UpdateBannerRequest request) {
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
        return bannerMapper.toResponse(bannerRepository.save(banner));
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
    public BannerResponse uploadImage(UUID id, MultipartFile file) {
        Banner banner = findById(id);
        String previousImageUrl = banner.getImageUrl();

        banner.setImageUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        banner = bannerRepository.save(banner);

        if (previousImageUrl != null) {
            fileStorageService.delete(previousImageUrl);
        }
        return bannerMapper.toResponse(banner);
    }

    @Override
    @Transactional
    public BannerResponse removeImage(UUID id) {
        Banner banner = findById(id);
        if (banner.getImageUrl() != null) {
            fileStorageService.delete(banner.getImageUrl());
            banner.setImageUrl(null);
            banner = bannerRepository.save(banner);
        }
        return bannerMapper.toResponse(banner);
    }

    private Banner findById(UUID id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
    }
}
