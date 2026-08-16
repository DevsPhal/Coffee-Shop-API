package org.group1.coffeeshopapi.common.utils;

import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtil {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private PageUtil() {
    }

    public static Pageable buildPageable(Integer page, Integer size, String sortBy, String direction) {
        int requestPage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int pageNumber = requestPage - 1;
        int pageSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Sort sort = (sortBy == null || sortBy.isBlank())
                ? Sort.unsorted()
                : Sort.by(resolveDirection(direction), sortBy);

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    public static <T> PaginatedResponse<T> toPaginatedResponse(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + DEFAULT_PAGE)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private static Sort.Direction resolveDirection(String direction) {
        return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
}
