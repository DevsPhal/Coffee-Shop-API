package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.product.dto.response.CustomerProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "quantityOnHand", source = "inventory.quantityOnHand")
    @Mapping(target = "reorderLevel", source = "inventory.reorderLevel")
    @Mapping(target = "discountActive", expression = "java(product.isDiscountActive(java.time.LocalDateTime.now()))")
    @Mapping(target = "finalPrice", expression = "java(product.getFinalPrice(java.time.LocalDateTime.now()))")
    @Mapping(target = "sizeOptions", source = "sizeOptions")
    @Mapping(target = "createdBy", source = "product.createdBy")
    @Mapping(target = "createdByName", source = "createdByActor.name")
    @Mapping(target = "createdByRole", source = "createdByActor.role")
    @Mapping(target = "updatedBy", source = "product.updatedBy")
    @Mapping(target = "updatedByName", source = "updatedByActor.name")
    @Mapping(target = "updatedByRole", source = "updatedByActor.role")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", source = "product.updatedAt")
    ProductResponse toResponse(Product product, Inventory inventory, ActorSummary createdByActor,
            ActorSummary updatedByActor, List<ProductSizeOptionResponse> sizeOptions);

    // Strips inventory counts, reorder thresholds, and staff audit identities before a product
    // reaches a customer — see CustomerProductResponse's javadoc.
    CustomerProductResponse toCustomerResponse(ProductResponse response);
}