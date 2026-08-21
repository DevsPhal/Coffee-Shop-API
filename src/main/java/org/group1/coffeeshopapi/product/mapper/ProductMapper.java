package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "quantityOnHand", source = "inventory.quantityOnHand")
    @Mapping(target = "reorderLevel", source = "inventory.reorderLevel")
    @Mapping(target = "discountActive", expression = "java(product.isDiscountActive(java.time.LocalDateTime.now()))")
    @Mapping(target = "finalPrice", expression = "java(product.getFinalPrice(java.time.LocalDateTime.now()))")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", source = "product.updatedAt")
    ProductResponse toResponse(Product product, Inventory inventory);
}