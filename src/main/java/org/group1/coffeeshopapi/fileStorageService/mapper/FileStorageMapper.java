package org.group1.coffeeshopapi.fileStorageService.mapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.group1.coffeeshopapi.fileStorageService.dto.response.ImageResponse;
import org.group1.coffeeshopapi.fileStorageService.entity.FileRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileStorageMapper {

    @Mapping(target = "objectName", source = "objectName")
    @Mapping(target = "url", source = "objectUrl")
    @Mapping(target = "uploadedAt", source = "createdAt")
    ImageResponse toResponse(FileRecord fileRecord);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FileRecord toEntity(ImageResponse response);

    default String buildUrl(String objectName, String bucketName, String publicEndpoint) {
        if (objectName == null || bucketName == null || publicEndpoint == null) {
            return null;
        }
        String encodedName = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        return String.format("%s/%s/%s", publicEndpoint, bucketName, encodedName);
    }
}
