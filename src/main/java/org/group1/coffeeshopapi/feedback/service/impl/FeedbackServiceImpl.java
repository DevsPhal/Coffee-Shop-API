package org.group1.coffeeshopapi.feedback.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.FeedbackStatus;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.feedback.dto.request.CreateFeedbackRequest;
import org.group1.coffeeshopapi.feedback.dto.response.FeedbackResponse;
import org.group1.coffeeshopapi.feedback.entity.Feedback;
import org.group1.coffeeshopapi.feedback.mapper.FeedbackMapper;
import org.group1.coffeeshopapi.feedback.repository.FeedbackRepository;
import org.group1.coffeeshopapi.feedback.service.FeedbackService;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CustomerRepository customerRepository;
    private final FeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public FeedbackResponse submit(CreateFeedbackRequest request, UUID customerId) {
        Feedback feedback = new Feedback();
        feedback.setCustomer(customerRepository.getReferenceById(customerId));
        feedback.setFullName(request.fullName());
        feedback.setEmail(request.email());
        feedback.setPhoneNumber(request.phoneNumber());
        feedback.setTopic(request.topic());
        feedback.setMessage(request.message());
        return toResponse(feedbackRepository.save(feedback));
    }

    @Override
    public Page<FeedbackResponse> list(FeedbackStatus status, Pageable pageable) {
        Page<Feedback> page = status != null
                ? feedbackRepository.findByStatus(status, pageable)
                : feedbackRepository.findAll(pageable);
        return page.map(feedbackMapper::toResponse);
    }

    @Override
    public FeedbackResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public FeedbackResponse updateStatus(UUID id, FeedbackStatus status) {
        Feedback feedback = findById(id);
        feedback.setStatus(status);
        return toResponse(feedbackRepository.save(feedback));
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return feedbackMapper.toResponse(feedback);
    }

    private Feedback findById(UUID id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
    }
}
