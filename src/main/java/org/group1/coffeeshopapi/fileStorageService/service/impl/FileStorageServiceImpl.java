package org.group1.coffeeshopapi.fileStorageService.service.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.InvalidFileException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.fileStorageService.dto.response.ImageResponse;
import org.group1.coffeeshopapi.fileStorageService.entity.FileRecord;
import org.group1.coffeeshopapi.fileStorageService.mapper.FileStorageMapper;
import org.group1.coffeeshopapi.fileStorageService.repository.FileRecordRepository;
import org.group1.coffeeshopapi.fileStorageService.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final MinioClient minioClient;
    private final FileStorageMapper fileStorageMapper;
    private final FileRecordRepository fileRecordRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Override
    public ImageResponse uploadImage(MultipartFile file) throws Exception {
        validateImageFile(file);
        String objectName = generateObjectName(file.getOriginalFilename());

        putObject(objectName, file);

        FileRecord record = new FileRecord();
        record.setObjectName(objectName);
        record.setOriginalFilename(file.getOriginalFilename());
        record.setContentType(file.getContentType());
        record.setSize(file.getSize());
        record.setBucketName(bucketName);
        record.setObjectUrl(buildObjectUrl(objectName));
        fileRecordRepository.save(record);

        return fileStorageMapper.toResponse(record);
    }

    @Override
    public ImageResponse updateImage(String objectName, MultipartFile file) throws Exception {
        validateImageFile(file);
        FileRecord record = findRecord(objectName);

        putObject(objectName, file);

        record.setOriginalFilename(file.getOriginalFilename());
        record.setContentType(file.getContentType());
        record.setSize(file.getSize());
        fileRecordRepository.save(record);

        return fileStorageMapper.toResponse(record);
    }

    @Override
    public byte[] getImage(String objectName) throws Exception {
        FileRecord record = findRecord(objectName);

        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(record.getObjectName())
                .build())) {
            return inputStream.readAllBytes();
        }
    }

    @Override
    public ImageResponse getImageMetadata(String objectName) {
        return fileStorageMapper.toResponse(findRecord(objectName));
    }

    @Override
    public List<ImageResponse> listImages() {
        return fileRecordRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(fileStorageMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteImage(String objectName) throws Exception {
        FileRecord record = findRecord(objectName);
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(record.getObjectName())
                .build());
        fileRecordRepository.delete(record);
    }

    private FileRecord findRecord(String objectName) {
        return fileRecordRepository.findByObjectName(objectName)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + objectName));
    }

    private void putObject(String objectName, MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Unsupported file type: " + contentType
                    + ". Allowed types: " + ALLOWED_CONTENT_TYPES);
        }
    }

    private String generateObjectName(String originalFilename) {
        return UUID.randomUUID() + extractExtension(originalFilename);
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex).toLowerCase() : "";
    }

    private String buildObjectUrl(String objectName) {
        return String.format("%s/%s/%s", publicEndpoint, bucketName, objectName);
    }
}
