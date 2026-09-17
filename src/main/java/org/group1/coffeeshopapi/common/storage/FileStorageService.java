package org.group1.coffeeshopapi.common.storage;

import org.springframework.web.multipart.MultipartFile;

// Shared file storage for images (product photos, avatars, banners, ...), currently backed by
// MinIO.
public interface FileStorageService {

    // Stores the file under the given folder and returns its public URL.
    String uploadImage(MultipartFile file, String folder);

    // Deletes a previously uploaded file. Safe to call with null.
    void delete(String fileUrl);
}