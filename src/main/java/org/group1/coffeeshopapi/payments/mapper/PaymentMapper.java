package org.group1.coffeeshopapi.payments.mapper;

import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;
import org.group1.coffeeshopapi.payments.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

}
