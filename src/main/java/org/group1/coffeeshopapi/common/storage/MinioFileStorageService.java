package org.group1.coffeeshopapi.common.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.FileStorageException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.MinioProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new InvalidOperationException("Image must not exceed 5MB");
        }
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw new InvalidOperationException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }

        String objectKey = folder + "/" + UUID.randomUUID() + extension;
        try (var inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload image", e);
        }

        return minioProperties.getPublicEndpoint() + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    @Override
    public void delete(String fileUrl) {
        String objectKey = toObjectKey(fileUrl);
        if (objectKey == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            // Best-effort: an orphaned object in MinIO is harmless, so failing to delete it
            // should never block the caller's own create/update/delete flow.
            log.warn("Failed to delete file '{}' from MinIO", fileUrl, e);
        }
    }

    private String toObjectKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        String prefix = minioProperties.getPublicEndpoint() + "/" + minioProperties.getBucket() + "/";
        if (!fileUrl.startsWith(prefix)) {
            return null;
        }
        return fileUrl.substring(prefix.length());
    }
}