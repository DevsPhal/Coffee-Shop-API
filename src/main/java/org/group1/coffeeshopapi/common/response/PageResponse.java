package org.group1.coffeeshopapi.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

// page is 1-based (page 1 = the first page), matching the request param PageUtil accepts —
// Spring Data's own Page is 0-based internally, so getNumber() is converted back here.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}