package org.group1.coffeeshopapi.extra.mapper;

import org.group1.coffeeshopapi.extra.dto.response.ExtraResponse;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExtraMapper {
    ExtraResponse toResponse(Extra extra);
}
