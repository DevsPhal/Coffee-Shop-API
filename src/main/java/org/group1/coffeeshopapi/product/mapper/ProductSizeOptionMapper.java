package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSizeOptionMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "finalPrice",
            expression = "java(sizeOption.getProduct().getFinalPrice(sizeOption.getPrice(), java.time.LocalDateTime.now()))")
    ProductSizeOptionResponse toResponse(ProductSizeOption sizeOption);
}
