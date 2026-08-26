package org.group1.coffeeshopapi.attendance.mapper;

import org.group1.coffeeshopapi.attendance.dto.response.AttendanceResponse;
import org.group1.coffeeshopapi.attendance.entity.Attendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(target = "baristaId", source = "barista.id")
    @Mapping(target = "baristaName", source = "barista.fullName")
    @Mapping(target = "open", expression = "java(attendance.getCheckOutAt() == null)")
    AttendanceResponse toResponse(Attendance attendance);
}
