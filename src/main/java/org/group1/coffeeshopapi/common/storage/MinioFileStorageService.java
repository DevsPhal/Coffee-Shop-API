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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

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

        // The client-supplied Content-Type is just metadata the client claims — Swagger UI,
        // Postman, and some browsers are inconsistent (or outright wrong) about setting it for
        // multipart uploads. Sniffing the actual file signature is both more reliable and safer
        // (never trust client-controlled metadata for a security-relevant decision).
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded image", e);
        }

        DetectedImageType detected = detectImageType(bytes);
        if (detected == null) {
            throw new InvalidOperationException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }

        String objectKey = folder + "/" + UUID.randomUUID() + detected.extension();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
                    .contentType(detected.contentType())
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload image", e);
        }

        return minioProperties.getPublicEndpoint() + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    private record DetectedImageType(String contentType, String extension) {
    }

    private DetectedImageType detectImageType(byte[] bytes) {
        if (startsWith(bytes, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) {
            return new DetectedImageType("image/jpeg", ".jpg");
        }
        if (startsWith(bytes, (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47)) {
            return new DetectedImageType("image/png", ".png");
        }
        if (startsWith(bytes, (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38)) {
            return new DetectedImageType("image/gif", ".gif");
        }
        if (isWebp(bytes)) {
            return new DetectedImageType("image/webp", ".webp");
        }
        return null;
    }

    private boolean startsWith(byte[] data, byte... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    // RIFF....WEBP: "RIFF" at offset 0, 4-byte chunk size, "WEBP" at offset 8.
    private boolean isWebp(byte[] data) {
        return data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
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