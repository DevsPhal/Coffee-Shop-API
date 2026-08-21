package org.group1.coffeeshopapi.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Shared object-storage gateway backing every entity's image field (Product, Event, User
 * avatar, ...). Keeping upload/delete behind one interface means every caller gets the same
 * validation and key-naming rules, and the storage backend (currently MinIO) can change without
 * touching entity code.
 */
public interface FileStorageService {

    /**
     * Validates and stores {@code file} under {@code folder}, returning the publicly reachable
     * URL of the stored object.
     */
    String uploadImage(MultipartFile file, String folder);

    /**
     * Best-effort delete of a previously uploaded file, addressed by the URL {@link #uploadImage}
     * returned. Safe to call with {@code null} or a URL this service didn't issue.
     */
    void delete(String fileUrl);
}