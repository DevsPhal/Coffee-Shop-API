package org.group1.coffeeshopapi.common.util;

import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilTest {

    @Test
    void capsOversizedPages() {
        assertThat(PageUtil.buildPageable(1, 1_000_000).getPageSize()).isEqualTo(AppConstant.MAX_PAGE_SIZE);
    }

    @Test
    void fallsBackToTheDefaultForMissingOrNonPositiveSizes() {
        assertThat(PageUtil.buildPageable(1, null).getPageSize()).isEqualTo(AppConstant.DEFAULT_PAGE_SIZE);
        assertThat(PageUtil.buildPageable(1, 0).getPageSize()).isEqualTo(AppConstant.DEFAULT_PAGE_SIZE);
        assertThat(PageUtil.buildPageable(1, -5).getPageSize()).isEqualTo(AppConstant.DEFAULT_PAGE_SIZE);
    }

    @Test
    void pagesAreOneBased() {
        assertThat(PageUtil.buildPageable(3, 10).getPageNumber()).isEqualTo(2);
        assertThat(PageUtil.buildPageable(0, 10).getPageNumber()).isZero();
    }
}
