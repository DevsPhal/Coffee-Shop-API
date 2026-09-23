package org.group1.coffeeshopapi.feedback.service;

import org.group1.coffeeshopapi.common.enums.FeedbackStatus;
import org.group1.coffeeshopapi.feedback.dto.request.CreateFeedbackRequest;
import org.group1.coffeeshopapi.feedback.dto.response.FeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FeedbackService {
    FeedbackResponse submit(CreateFeedbackRequest request, UUID customerId);

    Page<FeedbackResponse> list(FeedbackStatus status, Pageable pageable);
    FeedbackResponse getById(UUID id);
    FeedbackResponse updateStatus(UUID id, FeedbackStatus status);
}
