package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.common.util.InMemoryMultipartFile;
import org.group1.coffeeshopapi.telegram.dto.TelegramProfilePhoto;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramProfileSyncService;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramProfileSyncServiceImpl implements TelegramProfileSyncService {

    static final String AVATAR_FOLDER = "avatars";
    static final String WIDGET_PHOTO_PREFIX = "widget:";
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final TelegramApiClient telegramApiClient;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    private record Photo(String id, Supplier<byte[]> download) {
    }

    @Override
    public void sync(User user, Long telegramUserId, String telegramUsername, String widgetPhotoUrl) {
        boolean changed = syncUsername(user, telegramUsername);
        String replacedFile = null;
        try {
            String previousUrl = user.getAvatarUrl();
            boolean previousWasImported = isImported(user);
            if (syncAvatar(user, telegramUserId, widgetPhotoUrl)) {
                changed = true;
                if (previousWasImported && fileStorageService.isStoredFile(previousUrl)) {
                    replacedFile = previousUrl;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Kept the current avatar for user {}: the Telegram profile photo could not be synced ({})",
                    user.getId(), ex.getMessage());
        }

        if (changed) {
            userRepository.save(user);
        }
        if (replacedFile != null) {
            fileStorageService.delete(replacedFile);
        }
    }

    private boolean syncUsername(User user, String telegramUsername) {
        if (telegramUsername == null || telegramUsername.isBlank()
                || telegramUsername.equals(user.getTelegramUsername())) {
            return false;
        }
        user.setTelegramUsername(telegramUsername);
        return true;
    }

    private boolean syncAvatar(User user, Long telegramUserId, String widgetPhotoUrl) {
        if (hasCustomAvatar(user)) {
            return false;
        }

        Optional<Photo> current = currentPhoto(telegramUserId, widgetPhotoUrl);
        if (current.isEmpty()) {
            // Telegram says there's no photo (removed, or hidden by privacy settings): fall back to the default avatar.
            if (user.getAvatarUrl() == null && user.getTelegramPhotoId() == null) {
                return false;
            }
            user.setAvatarUrl(null);
            user.setTelegramPhotoId(null);
            return true;
        }

        Photo photo = current.get();
        if (photo.id().equals(user.getTelegramPhotoId()) && user.getAvatarUrl() != null) {
            return false;
        }
        String storedUrl = fileStorageService.uploadImage(
                new InMemoryMultipartFile("file", "telegram-avatar", null, photo.download().get()), AVATAR_FOLDER);
        user.setAvatarUrl(storedUrl);
        user.setTelegramPhotoId(photo.id());
        return true;
    }

    // A photo uploaded by hand always wins. Anything else (nothing yet, an earlier Telegram import, or a Telegram URL
    // linked directly by older versions) follows Telegram.
    private boolean hasCustomAvatar(User user) {
        return user.getAvatarUrl() != null && !isImported(user);
    }

    private boolean isImported(User user) {
        return user.getTelegramPhotoId() != null
                || (user.getAvatarUrl() != null && !fileStorageService.isStoredFile(user.getAvatarUrl()));
    }

    // Empty when Telegram reports no photo; throws when it couldn't be asked and there is nothing to fall back on.
    private Optional<Photo> currentPhoto(Long telegramUserId, String widgetPhotoUrl) {
        RuntimeException botFailure = null;
        if (telegramUserId != null) {
            try {
                Optional<TelegramProfilePhoto> found = telegramApiClient.findProfilePhoto(telegramUserId);
                if (found.isPresent()) {
                    TelegramProfilePhoto photo = found.get();
                    return Optional.of(new Photo(photo.fileUniqueId(),
                            () -> telegramApiClient.downloadFile(photo.fileId(), MAX_PHOTO_BYTES)));
                }
            } catch (RuntimeException ex) {
                botFailure = ex;
            }
        }
        if (widgetPhotoUrl != null && !widgetPhotoUrl.isBlank()) {
            // Widget photo URLs change whenever the picture does, so the URL itself identifies the photo.
            return Optional.of(new Photo(WIDGET_PHOTO_PREFIX + fingerprint(widgetPhotoUrl),
                    () -> telegramApiClient.downloadWidgetPhoto(widgetPhotoUrl, MAX_PHOTO_BYTES)));
        }
        if (botFailure != null) {
            throw botFailure;
        }
        return Optional.empty();
    }

    static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(Objects.requireNonNull(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
