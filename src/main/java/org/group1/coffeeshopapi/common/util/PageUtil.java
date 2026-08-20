package org.group1.coffeeshopapi.common.util;

import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a {@link Pageable} from optional request params, falling back to the defaults in
 * {@link AppConstant} (page {@value AppConstant#DEFAULT_PAGE_NUMBER}, size
 * {@value AppConstant#DEFAULT_PAGE_SIZE}, sorted by {@value AppConstant#DEFAULT_SORT_BY}
 * {@value AppConstant#DEFAULT_SORT_DIRECTION}) so every "list all" endpoint paginates the
 * same way without repeating those defaults.
 */
public final class PageUtil {

    private PageUtil() {
    }

    public static Pageable buildPageable(Integer page, Integer size) {
        return buildPageable(page, size, AppConstant.DEFAULT_SORT_BY, AppConstant.DEFAULT_SORT_DIRECTION);
    }

    public static Pageable buildPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        int pageNumber = page != null ? page : AppConstant.DEFAULT_PAGE_NUMBER;
        int pageSize = size != null ? size : AppConstant.DEFAULT_PAGE_SIZE;
        String property = sortBy != null ? sortBy : AppConstant.DEFAULT_SORT_BY;
        Sort.Direction direction = Sort.Direction.fromString(
                sortDirection != null ? sortDirection : AppConstant.DEFAULT_SORT_DIRECTION);
        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, property));
    }
}