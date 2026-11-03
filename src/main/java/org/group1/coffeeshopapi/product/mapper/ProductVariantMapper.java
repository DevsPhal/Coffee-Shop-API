package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.inventory.service.VariantStock;
import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    @Mapping(target = "id", source = "variant.id")
    @Mapping(target = "productId", source = "variant.product.id")
    @Mapping(target = "name", source = "variant.name")
    @Mapping(target = "price", source = "variant.price")
    @Mapping(target = "unitsPerSale", source = "variant.unitsPerSale")
    @Mapping(target = "sortOrder", source = "variant.sortOrder")
    @Mapping(target = "status", source = "variant.status")
    @Mapping(target = "sku", expression = "java(org.group1.coffeeshopapi.product.service.ProductSkuGenerator"
            + ".variantSku(variant.getProduct().getSku(), variant.getName()))")
    @Mapping(target = "finalPrice",
            expression = "java(variant.getProduct().getFinalPrice(variant.getPrice(), java.time.LocalDateTime.now()))")
    @Mapping(target = "availableQuantity", expression = "java(stock != null ? stock.available() : null)")
    @Mapping(target = "recipeBased", expression = "java(stock != null && stock.recipeBased())")
    ProductVariantResponse toResponse(ProductVariant variant, VariantStock stock);

    default ProductVariantResponse toResponse(ProductVariant variant) {
        return toResponse(variant, null);
    }
}
