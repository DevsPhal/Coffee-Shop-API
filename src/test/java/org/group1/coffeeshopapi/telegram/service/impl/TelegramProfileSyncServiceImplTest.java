package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.telegram.dto.TelegramProfilePhoto;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramProfileSyncServiceImplTest {

    private static final Long TELEGRAM_ID = 9001L;
    private static final String STORED_OLD = "https://cdn.example/590st-public/avatars/old.jpg";
    private static final String STORED_NEW = "https://cdn.example/590st-public/avatars/new.jpg";
    private static final String OWN_UPLOAD = "https://cdn.example/590st-public/avatars/own.jpg";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @Mock private TelegramApiClient telegramApiClient;
    @Mock private FileStorageService fileStorageService;
    @Mock private UserRepository userRepository;
    @InjectMocks private TelegramProfileSyncServiceImpl service;

    @Test
    void aFirstTelegramLoginCopiesTheProfilePhotoIntoOurStorage() {
        User user = user(null, null);
        telegramHasPhoto("photo-1");
        when(fileStorageService.uploadImage(any(MultipartFile.class), eq("avatars"))).thenReturn(STORED_NEW);

        service.sync(user, TELEGRAM_ID, null, null);

        assertThat(user.getAvatarUrl()).isEqualTo(STORED_NEW);
        assertThat(user.getTelegramPhotoId()).isEqualTo("photo-1");
        verify(userRepository).save(user);
    }

    @Test
    void anUnchangedPhotoIsNotDownloadedAgain() {
        User user = user(STORED_OLD, "photo-1");
        storedFiles(STORED_OLD);
        when(telegramApiClient.findProfilePhoto(TELEGRAM_ID))
                .thenReturn(Optional.of(new TelegramProfilePhoto("file-1", "photo-1")));

        service.sync(user, TELEGRAM_ID, null, null);

        assertThat(user.getAvatarUrl()).isEqualTo(STORED_OLD);
        verify(telegramApiClient, never()).downloadFile(anyString(), anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aNewTelegramPhotoReplacesTheOldCopyAndDeletesItOnlyAfterSaving() {
        User user = user(STORED_OLD, "photo-1");
        storedFiles(STORED_OLD);
        telegramHasPhoto("photo-2");
        when(fileStorageService.uploadImage(any(MultipartFile.class), eq("avatars"))).thenReturn(STORED_NEW);

        service.sync(user, TELEGRAM_ID, null, null);

        assertThat(user.getAvatarUrl()).isEqualTo(STORED_NEW);
        assertThat(user.getTelegramPhotoId()).isEqualTo("photo-2");
        InOrder order = inOrder(userRepository, fileStorageService);
        order.verify(userRepository).save(user);
        order.verify(fileStorageService).delete(STORED_OLD);
    }

    @Test
    void aPhotoUploadedByHandIsNeverReplaced() {
        User user = user(OWN_UPLOAD, null);
        storedFiles(OWN_UPLOAD);

        service.sync(user, TELEGRAM_ID, null, "https://t.me/i/userpic/320/alice.jpg");

        assertThat(user.getAvatarUrl()).isEqualTo(OWN_UPLOAD);
        verifyNoInteractions(telegramApiClient);
        verify(userRepository, never()).save(any());
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void aTelegramUrlLinkedDirectlyByOlderVersionsIsReplacedWithAStoredCopy() {
        String hotlinked = "https://t.me/i/userpic/320/alice.jpg";
        User user = user(hotlinked, null);
        storedFiles();
        telegramHasPhoto("photo-1");
        when(fileStorageService.uploadImage(any(MultipartFile.class), eq("avatars"))).thenReturn(STORED_NEW);

        service.sync(user, TELEGRAM_ID, null, null);

        assertThat(user.getAvatarUrl()).isEqualTo(STORED_NEW);
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void removingTheTelegramPhotoFallsBackToTheDefaultAvatar() {
        User user = user(STORED_OLD, "photo-1");
        storedFiles(STORED_OLD);
        when(telegramApiClient.findProfilePhoto(TELEGRAM_ID)).thenReturn(Optional.empty());

        service.sync(user, TELEGRAM_ID, null, null);

        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.getTelegramPhotoId()).isNull();
        verify(userRepository).save(user);
        verify(fileStorageService).delete(STORED_OLD);
    }

    @Test
    void anAccountWithoutAnyPhotoStaysOnTheDefaultAvatarWithoutBeingSaved() {
        User user = user(null, null);
        when(telegramApiClient.findProfilePhoto(TELEGRAM_ID)).thenReturn(Optional.empty());

        service.sync(user, TELEGRAM_ID, null, null);

        assertThat(user.getAvatarUrl()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void whenTheBotApiFailsTheSignedWidgetPhotoIsUsedInstead() {
        String photoUrl = "https://t.me/i/userpic/320/alice.jpg";
        User user = user(null, null);
        when(telegramApiClient.findProfilePhoto(TELEGRAM_ID)).thenThrow(unavailable());
        when(telegramApiClient.downloadWidgetPhoto(eq(photoUrl), anyLong())).thenReturn(JPEG);
        when(fileStorageService.uploadImage(any(MultipartFile.class), eq("avatars"))).thenReturn(STORED_NEW);

        service.sync(user, TELEGRAM_ID, null, photoUrl);

        assertThat(user.getAvatarUrl()).isEqualTo(STORED_NEW);
        assertThat(user.getTelegramPhotoId())
                .isEqualTo("widget:" + TelegramProfileSyncServiceImpl.fingerprint(photoUrl));
    }

    @Test
    void whenTelegramCannotBeReachedTheCurrentAvatarIsKeptAndLoginCarriesOn() {
        User user = user(STORED_OLD, "photo-1");
        when(telegramApiClient.findProfilePhoto(TELEGRAM_ID)).thenThrow(unavailable());

        assertThatCode(() -> service.sync(user, TELEGRAM_ID, null, null)).doesNotThrowAnyException();

        assertThat(user.getAvatarUrl()).isEqualTo(STORED_OLD);
        assertThat(user.getTelegramPhotoId()).isEqualTo("photo-1");
        verify(userRepository, never()).save(any());
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void aPhotoThatIsNotAnImageKeepsTheCurrentAvatarButStillSavesTheNewUsername() {
        User user = user(null, null);
        user.setTelegramUsername("old_handle");
        telegramHasPhoto("photo-1");
        when(fileStorageService.uploadImage(any(MultipartFile.class), eq("avatars")))
                .thenThrow(new InvalidOperationException("Unsupported image type"));

        assertThatCode(() -> service.sync(user, TELEGRAM_ID, "new_handle", null)).doesNotThrowAnyException();

        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.getTelegramPhotoId()).isNull();
        assertThat(user.getTelegramUsername()).isEqualTo("new_handle");
        verify(userRepository).save(user);
    }

    @Test
    void aBlankUsernameFromTelegramNeverWipesTheStoredOne() {
        User user = user(OWN_UPLOAD, null);
        user.setTelegramUsername("alice");
        storedFiles(OWN_UPLOAD);

        service.sync(user, TELEGRAM_ID, " ", null);

        assertThat(user.getTelegramUsername()).isEqualTo("alice");
        verify(userRepository, never()).save(any());
    }

    private void telegramHasPhoto(String uniqueId) {
        when(telegramApiClient.findProfilePhoto(TELEGRAM_ID))
                .thenReturn(Optional.of(new TelegramProfilePhoto("file-" + uniqueId, uniqueId)));
        when(telegramApiClient.downloadFile(eq("file-" + uniqueId), anyLong())).thenReturn(JPEG);
    }

    private void storedFiles(String... urls) {
        Set<String> stored = Set.of(urls);
        lenient().when(fileStorageService.isStoredFile(anyString()))
                .thenAnswer(invocation -> stored.contains(invocation.<String>getArgument(0)));
    }

    private static TelegramDeliveryException unavailable() {
        return new TelegramDeliveryException(TelegramDeliveryException.Reason.UNAVAILABLE, "Telegram is down");
    }

    private static User user(String avatarUrl, String telegramPhotoId) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFullName("Alice Smith");
        customer.setAvatarUrl(avatarUrl);
        customer.setTelegramPhotoId(telegramPhotoId);
        return customer;
    }
}
