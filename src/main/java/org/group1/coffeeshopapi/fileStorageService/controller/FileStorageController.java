package org.group1.coffeeshopapi.fileStorageService.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ImageResponse uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        return fileStorageService.uploadImage(file);
    }

    @PutMapping(value = "/{objectName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ImageResponse updateImage(
            @PathVariable String objectName,
            @RequestParam("file") MultipartFile file) throws Exception {
        return fileStorageService.updateImage(objectName, file);
    }

    @GetMapping("/{objectName}/metadata")
    public ImageResponse getImageMetadata(@PathVariable String objectName) throws Exception {
        return fileStorageService.getImageMetadata(objectName);
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
    public List<ImageResponse> listImages() throws Exception {
        return fileStorageService.listImages();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{objectName}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteImage(@PathVariable String objectName) throws Exception {
        fileStorageService.deleteImage(objectName);
    }
}
