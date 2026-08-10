package org.group1.coffeeshopapi.fileStorageService.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponse {
    private String objectName;
    private String originalFilename;
    private String url;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt;
}
