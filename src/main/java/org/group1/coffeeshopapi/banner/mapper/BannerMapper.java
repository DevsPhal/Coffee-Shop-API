package org.group1.coffeeshopapi.banner.mapper;

import org.group1.coffeeshopapi.banner.dto.response.BannerResponse;
import org.group1.coffeeshopapi.banner.entity.Banner;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BannerMapper {

    @Mapping(target = "id", source = "banner.id")
    @Mapping(target = "adminName", source = "adminActor.name")
    @Mapping(target = "adminRole", source = "adminActor.role")
    @Mapping(target = "updatedByAdminName", source = "updatedByAdminActor.name")
    @Mapping(target = "updatedByAdminRole", source = "updatedByAdminActor.role")
    BannerResponse toResponse(Banner banner, ActorSummary adminActor, ActorSummary updatedByAdminActor);
}
