package org.group1.coffeeshopapi.fileStorageService.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UploadImageRequest {
    private String objectName;
    private String contentType;
    private byte[] fileBytes;
}
