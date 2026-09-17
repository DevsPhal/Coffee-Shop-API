package org.group1.coffeeshopapi.banner.service.impl;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.banner.dto.request.CreateBannerRequest;
import org.group1.coffeeshopapi.banner.entity.Banner;
import org.group1.coffeeshopapi.banner.mapper.BannerMapper;
import org.group1.coffeeshopapi.banner.repository.BannerRepository;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the two bits of real logic in an otherwise thin CRUD service: the default sort order
// and deleting the previous image on replace/remove.
@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private BannerMapper bannerMapper;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private BannerServiceImpl service;

    @Test
    void creatingABannerWithNoSortOrderDefaultsToZero() {
        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateBannerRequest("Summer Sale", null, null), new Admin());

        ArgumentCaptor<Banner> captor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isZero();
    }

    @Test
    void uploadingANewImageDeletesThePreviousOne() {
        UUID id = UUID.randomUUID();
        Banner banner = new Banner();
        banner.setId(id);
        banner.setImageUrl("old-url");
        when(bannerRepository.findById(id)).thenReturn(Optional.of(banner));
        when(fileStorageService.uploadImage(any(), any())).thenReturn("new-url");
        when(bannerRepository.save(banner)).thenReturn(banner);

        service.uploadImage(id, null, new Admin());

        assertThat(banner.getImageUrl()).isEqualTo("new-url");
        verify(fileStorageService).delete("old-url");
    }

    @Test
    void removingAnImageThatIsAlreadyClearIsANoOp() {
        UUID id = UUID.randomUUID();
        Banner banner = new Banner();
        banner.setId(id);
        banner.setImageUrl(null);
        when(bannerRepository.findById(id)).thenReturn(Optional.of(banner));

        service.removeImage(id, new Admin());

        verify(fileStorageService, never()).delete(any());
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void deletingABannerAlsoDeletesItsImageFile() {
        UUID id = UUID.randomUUID();
        Banner banner = new Banner();
        banner.setId(id);
        banner.setImageUrl("some-url");
        when(bannerRepository.findById(id)).thenReturn(Optional.of(banner));

        service.delete(id);

        verify(fileStorageService).delete("some-url");
        verify(bannerRepository).delete(banner);
    }
}
