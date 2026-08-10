package org.group1.coffeeshopapi.notifications.mapper;

import org.group1.coffeeshopapi.notifications.dto.response.NotificationResponse;
import org.group1.coffeeshopapi.notifications.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
