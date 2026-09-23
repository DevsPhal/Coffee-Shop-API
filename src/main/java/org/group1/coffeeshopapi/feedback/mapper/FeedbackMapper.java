package org.group1.coffeeshopapi.feedback.mapper;

import org.group1.coffeeshopapi.feedback.dto.response.FeedbackResponse;
import org.group1.coffeeshopapi.feedback.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "customerId", source = "feedback.customer.id")
    @Mapping(target = "customerName", source = "feedback.customer.fullName")
    FeedbackResponse toResponse(Feedback feedback);
}
