package org.group1.coffeeshopapi.inventory.service;

import org.group1.coffeeshopapi.inventory.dto.response.StockInImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StockInImportService {
    byte[] generateTemplate();

    StockInImportResponse importFromExcel(MultipartFile file, UUID performedBy);
}
