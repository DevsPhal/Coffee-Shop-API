package org.group1.coffeeshopapi.extra.mapper;

import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductExtraMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "extraId", source = "extra.id")
    @Mapping(target = "name", source = "extra.name")
    @Mapping(target = "price", source = "extra.price")
    @Mapping(target = "quantityOnHand", source = "extra.quantityOnHand")
    ProductExtraResponse toResponse(ProductExtra productExtra);
}
