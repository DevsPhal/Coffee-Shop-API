package org.group1.coffeeshopapi.event.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.event.dto.response.CustomerEventResponse;
import org.group1.coffeeshopapi.event.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// No auth required — the Telegram bot's /events command already serves this same content to
// anyone, linked account or not (see EventsCommand), same as PublicBannerController.
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Public: upcoming promotional/announcement events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ApiResponse<List<CustomerEventResponse>> listUpcoming() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, eventService.listUpcoming());
    }
}
