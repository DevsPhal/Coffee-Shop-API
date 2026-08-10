package org.group1.coffeeshopapi.staff.service;

import java.util.List;

import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;

public interface StaffService {

    List<StaffResponse> getAllStaff();

    StaffResponse getStaffById(Long id);

    StaffResponse createStaff(StaffRequest request);

    StaffResponse updateStaff(Long id, StaffRequest request);

    void deleteStaff(Long id);
}
