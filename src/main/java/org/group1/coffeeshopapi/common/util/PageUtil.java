package org.group1.coffeeshopapi.common.util;

import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtil {

    private PageUtil() {
    }

    public static Pageable buildPageable(Integer page, Integer size) {
        return buildPageable(page, size, AppConstant.DEFAULT_SORT_BY, AppConstant.DEFAULT_SORT_DIRECTION);
    }

    public static Pageable buildPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        int requestedPage = page != null ? page : AppConstant.DEFAULT_PAGE_NUMBER;
        int pageIndex = Math.max(requestedPage - 1, 0);
        int pageSize = size == null || size < 1 ? AppConstant.DEFAULT_PAGE_SIZE : Math.min(size, AppConstant.MAX_PAGE_SIZE);
        String property = sortBy != null ? sortBy : AppConstant.DEFAULT_SORT_BY;
        Sort.Direction direction = Sort.Direction.fromString(
                sortDirection != null ? sortDirection : AppConstant.DEFAULT_SORT_DIRECTION);
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, property));
    }
}
