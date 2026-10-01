package org.group1.coffeeshopapi.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadImage(MultipartFile file, String folder);

    void delete(String fileUrl);
}
