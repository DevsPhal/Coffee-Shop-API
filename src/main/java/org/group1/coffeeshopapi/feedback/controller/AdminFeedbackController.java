package org.group1.coffeeshopapi.feedback.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.FeedbackStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.feedback.dto.request.UpdateFeedbackStatusRequest;
import org.group1.coffeeshopapi.feedback.dto.response.FeedbackResponse;
import org.group1.coffeeshopapi.feedback.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@Tag(name = "Admin Feedback", description = "Admin only: review messages sent through Contact Us")
@SecurityRequirement(name = "bearerAuth")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ApiResponse<PageResponse<FeedbackResponse>> list(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(feedbackService.list(status, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<FeedbackResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, feedbackService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<FeedbackResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateFeedbackStatusRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Feedback status updated successfully.",
                feedbackService.updateStatus(id, request.status()));
    }
}
