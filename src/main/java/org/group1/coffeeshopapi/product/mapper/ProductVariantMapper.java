package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "sku", expression = "java(org.group1.coffeeshopapi.product.service.ProductSkuGenerator"
            + ".variantSku(variant.getProduct().getSku(), variant.getName()))")
    @Mapping(target = "finalPrice",
            expression = "java(variant.getProduct().getFinalPrice(variant.getPrice(), java.time.LocalDateTime.now()))")
    ProductVariantResponse toResponse(ProductVariant variant);
}
