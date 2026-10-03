package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.table.dto.response.TableActivityResponse;

import java.util.List;
import java.util.UUID;

public interface TableActivityService {
    List<TableActivityResponse> list(TableStatus status);
    TableActivityResponse getById(UUID id);
}
