package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSizeOptionMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductSizeOptionResponse toResponse(ProductSizeOption sizeOption);
}
