package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.product.dto.response.ProductImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImportService {
    byte[] generateTemplate();

    ProductImportResponse importFromExcel(MultipartFile file, Admin actorAdmin);
}
