package org.group1.coffeeshopapi.pos.mapper;

import org.group1.coffeeshopapi.pos.dto.request.PosItemRequest;
import org.group1.coffeeshopapi.pos.dto.response.PosItemResponse;
import org.group1.coffeeshopapi.pos.entity.PosItem;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PosItemMapper {
    PosItem toEntity(PosItemRequest request);

    PosItemResponse toResponse(PosItem item);

    void updateEntity(PosItemRequest request, @MappingTarget PosItem item);
}
