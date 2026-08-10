package org.group1.coffeeshopapi.fileStorageService.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.fileStorageService.dto.response.ImageResponse;
import org.group1.coffeeshopapi.fileStorageService.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageResponse>> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        ImageResponse response = fileStorageService.uploadImage(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ImageResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Image uploaded successfully")
                        .data(response)
                        .timeStamp(LocalDateTime.now())
                        .build());
    }

    @PutMapping(value = "/{objectName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageResponse>> updateImage(
            @PathVariable String objectName,
            @RequestParam("file") MultipartFile file) throws Exception {
        ImageResponse response = fileStorageService.updateImage(objectName, file);
        return ResponseEntity.ok(ApiResponse.<ImageResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Image updated successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{objectName}/metadata")
    public ResponseEntity<ApiResponse<ImageResponse>> getImageMetadata(@PathVariable String objectName) throws Exception {
        ImageResponse response = fileStorageService.getImageMetadata(objectName);
        return ResponseEntity.ok(ApiResponse.<ImageResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Image metadata retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{objectName}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String objectName) throws Exception {
        byte[] imageBytes = fileStorageService.getImage(objectName);
        ImageResponse metadata = fileStorageService.getImageMetadata(objectName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, metadata.getContentType())
                .body(imageBytes);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImageResponse>>> listImages() throws Exception {
        List<ImageResponse> images = fileStorageService.listImages();
        return ResponseEntity.ok(ApiResponse.<List<ImageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Image list retrieved successfully")
                .data(images)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{objectName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String objectName) throws Exception {
        fileStorageService.deleteImage(objectName);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Image deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
