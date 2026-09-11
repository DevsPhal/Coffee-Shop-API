package org.group1.coffeeshopapi.banner.mapper;

import org.group1.coffeeshopapi.banner.dto.response.BannerResponse;
import org.group1.coffeeshopapi.banner.entity.Banner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BannerMapper {

    // admin/updatedByAdmin are null both for pre-existing rows and for a change made by the
    // Super Admin (see Banner's javadoc) — MapStruct null-checks the nested path automatically,
    // so adminName/adminRole simply come out null too in that case.
    @Mapping(target = "id", source = "banner.id")
    @Mapping(target = "adminId", source = "banner.admin.id")
    @Mapping(target = "adminName", source = "banner.admin.fullName")
    @Mapping(target = "adminRole", source = "banner.admin.role")
    @Mapping(target = "updatedByAdminId", source = "banner.updatedByAdmin.id")
    @Mapping(target = "updatedByAdminName", source = "banner.updatedByAdmin.fullName")
    @Mapping(target = "updatedByAdminRole", source = "banner.updatedByAdmin.role")
    BannerResponse toResponse(Banner banner);

    // The public, unauthenticated listing (storefront landing page) — deliberately doesn't
    // expose staff identities to anonymous visitors, only the raw ids (for parity with the
    // admin-facing response shape).
    @Mapping(target = "id", source = "banner.id")
    @Mapping(target = "adminId", source = "banner.admin.id")
    @Mapping(target = "adminName", ignore = true)
    @Mapping(target = "adminRole", ignore = true)
    @Mapping(target = "updatedByAdminId", source = "banner.updatedByAdmin.id")
    @Mapping(target = "updatedByAdminName", ignore = true)
    @Mapping(target = "updatedByAdminRole", ignore = true)
    BannerResponse toPublicResponse(Banner banner);
}
