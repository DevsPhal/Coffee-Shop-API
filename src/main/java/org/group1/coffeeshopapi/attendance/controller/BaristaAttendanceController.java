package org.group1.coffeeshopapi.attendance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceResponse;
import org.group1.coffeeshopapi.attendance.service.AttendanceService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barista/attendance")
@RequiredArgsConstructor
@Tag(name = "Barista Attendance", description = "Barista only: check in/out of a daily work shift and view your own attendance history")
@SecurityRequirement(name = "bearerAuth")
public class BaristaAttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ApiResponse<AttendanceResponse> checkIn(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Checked in successfully.", attendanceService.checkIn(currentUser.getId()));
    }

    @PostMapping("/check-out")
    public ApiResponse<AttendanceResponse> checkOut(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Checked out successfully.", attendanceService.checkOut(currentUser.getId()));
    }

    @GetMapping("/current")
    public ApiResponse<AttendanceResponse> getCurrentOpenShift(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                attendanceService.getCurrentOpenShift(currentUser.getId()));
    }

    @GetMapping
    public ApiResponse<PageResponse<AttendanceResponse>> listOwn(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(attendanceService.listOwn(currentUser.getId(), PageUtil.buildPageable(page, size))));
    }
}
