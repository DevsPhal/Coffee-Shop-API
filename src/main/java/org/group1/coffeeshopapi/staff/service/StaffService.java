package org.group1.coffeeshopapi.staff.service;

import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;
import org.springframework.data.domain.Pageable;

public interface StaffService {

    PageResponse<StaffResponse> getAllStaff(Pageable pageable);

    StaffResponse getStaffById(UUID id);

    StaffResponse createStaff(StaffRequest request);

    StaffResponse updateStaff(UUID id, StaffRequest request);

    void deleteStaff(UUID id);
}
