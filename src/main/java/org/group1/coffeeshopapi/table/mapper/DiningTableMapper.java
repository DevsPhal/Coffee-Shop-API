package org.group1.coffeeshopapi.table.mapper;

import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiningTableMapper {

    @Mapping(target = "id", source = "table.id")
    @Mapping(target = "createdAt", source = "table.createdAt")
    @Mapping(target = "updatedAt", source = "table.updatedAt")
    TableResponse toResponse(DiningTable table, String scanUrl);
}
