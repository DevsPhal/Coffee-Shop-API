package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.product.dto.response.CustomerProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "categoryGroup", source = "product.category.categoryGroup")
    @Mapping(target = "quantityOnHand", source = "inventory.quantityOnHand")
    @Mapping(target = "reorderLevel", source = "inventory.reorderLevel")
    @Mapping(target = "discountActive", expression = "java(product.isDiscountActive(java.time.LocalDateTime.now()))")
    @Mapping(target = "variants", source = "variants")
    @Mapping(target = "extras", source = "extras")
    @Mapping(target = "createdBy", source = "product.createdByAdmin.id")
    @Mapping(target = "createdByName", source = "product.createdByAdmin.fullName")
    @Mapping(target = "createdByRole", source = "product.createdByAdmin.role")
    @Mapping(target = "updatedBy", source = "product.updatedByAdmin.id")
    @Mapping(target = "updatedByName", source = "product.updatedByAdmin.fullName")
    @Mapping(target = "updatedByRole", source = "product.updatedByAdmin.role")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", source = "product.updatedAt")
    ProductResponse toResponse(Product product, Inventory inventory, List<ProductVariantResponse> variants,
            List<ProductExtraResponse> extras);

    // Strips internal fields before a product reaches a customer, and hides any out-of-stock
    // extra entirely rather than offering a choice they can't actually have.
    @Mapping(target = "extras", qualifiedByName = "inStockOnly")
    CustomerProductResponse toCustomerResponse(ProductResponse response);

    @Named("inStockOnly")
    default List<ProductExtraResponse> inStockOnly(List<ProductExtraResponse> extras) {
        return extras.stream()
                .filter(extra -> extra.quantityOnHand() == null || extra.quantityOnHand().signum() > 0)
                .toList();
    }
}