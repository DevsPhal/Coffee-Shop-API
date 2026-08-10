package org.group1.coffeeshopapi.setting.mapper;

import org.group1.coffeeshopapi.setting.dto.request.SettingRequest;
import org.group1.coffeeshopapi.setting.dto.response.SettingResponse;
import org.group1.coffeeshopapi.setting.entity.Setting;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SettingMapper {

    SettingResponse toResponse(Setting setting);

    Setting toEntity(SettingRequest request);
}
