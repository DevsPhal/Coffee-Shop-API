package org.group1.coffeeshopapi.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadImage(MultipartFile file, String folder);

    void delete(String fileUrl);

    /** Whether the URL points at a file this service stored, as opposed to an image hosted elsewhere. */
    boolean isStoredFile(String fileUrl);
}
