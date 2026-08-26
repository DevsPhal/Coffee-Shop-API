package org.group1.coffeeshopapi.attendance.mapper;

import org.group1.coffeeshopapi.attendance.dto.response.AttendanceAuditLogResponse;
import org.group1.coffeeshopapi.attendance.entity.AttendanceAuditLog;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceAuditLogMapper {

    @Mapping(target = "id", source = "log.id")
    @Mapping(target = "attendanceId", source = "log.attendance.id")
    @Mapping(target = "actorName", source = "actor.name")
    @Mapping(target = "actorRole", source = "actor.role")
    AttendanceAuditLogResponse toResponse(AttendanceAuditLog log, ActorSummary actor);
}
