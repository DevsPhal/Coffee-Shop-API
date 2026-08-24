package org.group1.coffeeshopapi.common.constant;

public class AppConstant {
    private AppConstant(){}

    // 1-based from the API's point of view — PageUtil converts to Spring Data's 0-based index.
    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final int DEFAULT_PAGE_SIZE = 9;

    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    public static final String SUCCESS_MESSAGE = "Request successful";
    public static final String NOT_FOUND_MESSAGE = "Resource not found";
}