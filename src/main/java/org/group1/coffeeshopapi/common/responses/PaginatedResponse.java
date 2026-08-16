package org.group1.coffeeshopapi.common.responses;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaginatedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
