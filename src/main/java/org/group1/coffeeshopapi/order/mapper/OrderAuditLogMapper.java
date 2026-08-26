package org.group1.coffeeshopapi.order.mapper;

import org.group1.coffeeshopapi.order.dto.response.OrderAuditLogResponse;
import org.group1.coffeeshopapi.order.entity.OrderAuditLog;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderAuditLogMapper {

    @Mapping(target = "id", source = "log.id")
    @Mapping(target = "actorName", source = "actor.name")
    @Mapping(target = "actorRole", source = "actor.role")
    OrderAuditLogResponse toResponse(OrderAuditLog log, ActorSummary actor);
}
