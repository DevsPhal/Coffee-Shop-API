package org.group1.coffeeshopapi.attendance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.attendance.dto.request.CreateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.dto.request.UpdateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceAuditLogResponse;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceResponse;
import org.group1.coffeeshopapi.attendance.service.AttendanceService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
@Tag(name = "Admin Attendance", description = "Admin only: view every barista's attendance, backfill a missed punch, "
        + "correct an existing record, and audit who touched what")
@SecurityRequirement(name = "bearerAuth")
public class AdminAttendanceController {

    private final AttendanceService attendanceService;
    private final CurrentActor currentActor;

    @GetMapping
    public ApiResponse<PageResponse<AttendanceResponse>> list(
            @RequestParam(required = false) UUID baristaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(attendanceService.listAll(baristaId, from, to, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<AttendanceResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, attendanceService.getById(id));
    }

    // Backfills a shift a barista forgot to punch in the app (or punched outside it entirely).
    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceResponse>> create(@Valid @RequestBody CreateAttendanceRequest request) {
        AttendanceResponse response = attendanceService.create(request, currentActor.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Attendance record created successfully.", response));
    }

    // Corrects an existing record's times/note — e.g. closing a shift a barista forgot to check out of.
    @PatchMapping("/{id}")
    public ApiResponse<AttendanceResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateAttendanceRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Attendance record updated successfully.",
                attendanceService.update(id, request, currentActor.id()));
    }

    // Full audit trail for one attendance record — check-in, check-out, and any admin backfill/correction.
    @GetMapping("/{id}/history")
    public ApiResponse<List<AttendanceAuditLogResponse>> getHistory(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, attendanceService.getHistory(id));
    }
}
