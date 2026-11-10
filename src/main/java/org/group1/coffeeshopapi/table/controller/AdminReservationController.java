package org.group1.coffeeshopapi.table.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.table.dto.request.CheckInReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.CloseReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.ReservationRequest;
import org.group1.coffeeshopapi.table.dto.response.ReservationResponse;
import org.group1.coffeeshopapi.table.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@Tag(name = "Admin Reservations", description = "Admin only: book tables, seat arriving parties and override table rules")
@SecurityRequirement(name = "bearerAuth")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final CurrentActor currentActor;

    @GetMapping
    public ApiResponse<List<ReservationResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) UUID tableId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, reservationService.list(date, status, tableId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, reservationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> create(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.create(request, currentActor.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Table booked successfully.", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReservationResponse> update(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Booking updated successfully.",
                reservationService.update(id, request, currentActor.id()));
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<ReservationResponse> checkIn(
            @PathVariable UUID id, @Valid @RequestBody(required = false) CheckInReservationRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Party seated.",
                reservationService.checkIn(id, request, currentActor.id(), true));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ReservationResponse> cancel(
            @PathVariable UUID id, @Valid @RequestBody(required = false) CloseReservationRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Booking cancelled.",
                reservationService.cancel(id, request, currentActor.id()));
    }

    @PostMapping("/{id}/no-show")
    public ApiResponse<ReservationResponse> markNoShow(
            @PathVariable UUID id, @Valid @RequestBody(required = false) CloseReservationRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Booking marked as a no-show.",
                reservationService.markNoShow(id, request, currentActor.id()));
    }
}
