package org.group1.coffeeshopapi.order.controller;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderChannel;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.properties.CorsProperties;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.config.SecurityConfig;
import org.group1.coffeeshopapi.order.dto.request.OrderSearchFilter;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.ReceiptService;
import org.group1.coffeeshopapi.order.service.StaffCallService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {BaristaOrderController.class, AdminOrderController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, CorsProperties.class})
class OrderListControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orderService;
    @MockitoBean ReceiptService receiptService;
    @MockitoBean StaffCallService staffCallService;
    @MockitoBean CurrentActor currentActor;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean StringRedisTemplate redisTemplate;

    @Test
    void baristaListPassesEveryFilterAndTheSortDirectionToTheSearch() throws Exception {
        when(orderService.search(any(), any())).thenReturn(Page.empty());

        mvc.perform(get("/api/barista/orders/all").with(user("barista").roles("BARISTA"))
                        .param("status", "COMPLETED,DELIVERED")
                        .param("search", "#A1B2")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-30")
                        .param("channel", "ONLINE")
                        .param("fulfillment", "DELIVERY")
                        .param("paymentMethod", "CASH")
                        .param("direction", "ASC")
                        .param("page", "2")
                        .param("size", "24"))
                .andExpect(status().isOk());

        verify(orderService).search(
                new OrderSearchFilter(null, null, List.of(OrderStatus.COMPLETED, OrderStatus.DELIVERED), "#A1B2",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), OrderChannel.ONLINE,
                        FulfillmentMethod.DELIVERY, PaymentMethod.CASH),
                PageRequest.of(1, 24, Sort.by(Sort.Direction.ASC, "createdAt")));
    }

    @Test
    void baristaListKeepsItsSingleStatusRequestAndNewestFirstDefault() throws Exception {
        when(orderService.search(any(), any())).thenReturn(Page.empty());

        mvc.perform(get("/api/barista/orders/all").with(user("barista").roles("BARISTA"))
                        .param("status", "PENDING").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService).search(
                new OrderSearchFilter(null, null, List.of(OrderStatus.PENDING), null, null, null, null, null, null),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void baristaStatusSummaryIsNotMistakenForAnOrderId() throws Exception {
        when(orderService.summarizeByStatus(any())).thenReturn(List.of());

        mvc.perform(get("/api/barista/orders/all/status-summary").with(user("barista").roles("BARISTA"))
                        .param("search", "dara")
                        .param("channel", "WALK_IN"))
                .andExpect(status().isOk());

        verify(orderService).summarizeByStatus(
                new OrderSearchFilter(null, null, null, "dara", null, null, OrderChannel.WALK_IN, null, null));
    }

    @Test
    void adminListAndSummaryAcceptTheSameFiltersPlusStaffAndCustomer() throws Exception {
        UUID baristaId = UUID.randomUUID();
        when(orderService.search(any(), any())).thenReturn(Page.empty());
        when(orderService.summarizeByStatus(any())).thenReturn(List.of());

        mvc.perform(get("/api/admin/orders").with(user("admin").roles("ADMIN"))
                        .param("baristaId", baristaId.toString())
                        .param("status", "PAID")
                        .param("status", "PREPARING")
                        .param("paymentMethod", "BAKONG"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/orders/status-summary").with(user("admin").roles("ADMIN"))
                        .param("from", "2026-10-30")
                        .param("to", "2026-10-30"))
                .andExpect(status().isOk());

        verify(orderService).search(
                new OrderSearchFilter(baristaId, null, List.of(OrderStatus.PAID, OrderStatus.PREPARING), null,
                        null, null, null, null, PaymentMethod.BAKONG),
                PageRequest.of(0, 9, Sort.by(Sort.Direction.DESC, "createdAt")));
        LocalDate day = LocalDate.of(2026, 10, 30);
        verify(orderService).summarizeByStatus(
                new OrderSearchFilter(null, null, null, null, day, day, null, null, null));
    }

    @Test
    void rejectsAnUnknownSortDirectionOrChannel() throws Exception {
        mvc.perform(get("/api/barista/orders/all").with(user("barista").roles("BARISTA"))
                        .param("direction", "SIDEWAYS"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/barista/orders/all/status-summary").with(user("barista").roles("BARISTA"))
                        .param("channel", "PHONE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void invoiceIsPrintedInEnglishUnlessStaffPickKhmer() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(receiptService.generateInvoicePdf(any(), any())).thenReturn(new byte[]{1});

        mvc.perform(get("/api/admin/orders/{id}/invoice", orderId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/barista/orders/{id}/invoice", orderId).with(user("barista").roles("BARISTA"))
                        .param("language", "KH"))
                .andExpect(status().isOk());

        verify(receiptService).generateInvoicePdf(orderId, ReceiptLanguage.EN);
        verify(receiptService).generateInvoicePdf(orderId, ReceiptLanguage.KH);
    }

    @Test
    void rejectsAnInvoiceLanguageThatCannotBePrinted() throws Exception {
        mvc.perform(get("/api/admin/orders/{id}/invoice", UUID.randomUUID()).with(user("admin").roles("ADMIN"))
                        .param("language", "FR"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(receiptService);
    }
}
