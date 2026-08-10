package org.group1.coffeeshopapi.fileStorageService.service;

import org.group1.coffeeshopapi.fileStorageService.dto.response.ImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    ImageResponse uploadImage(MultipartFile file) throws Exception;

    ImageResponse updateImage(String objectName, MultipartFile file) throws Exception;

    byte[] getImage(String objectName) throws Exception;

    ImageResponse getImageMetadata(String objectName) throws Exception;

    List<ImageResponse> listImages() throws Exception;

    void deleteImage(String objectName) throws Exception;
}
