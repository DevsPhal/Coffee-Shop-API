package org.group1.coffeeshopapi.config;

import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the production filter chain; the probe isolates authorization from business validation. */
@WebMvcTest
@ContextConfiguration(classes = SecurityPermissionsTest.ProbeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
class SecurityPermissionsTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean StringRedisTemplate redisTemplate;

    private static final Set<String> MANAGERS = Set.of("SUPER_ADMIN", "ADMIN");
    private static final Set<String> STAFF = Set.of("SUPER_ADMIN", "ADMIN", "BARISTA");
    private static final Set<String> SUPER_ADMIN = Set.of("SUPER_ADMIN");
    private static final Set<String> BARISTA = Set.of("BARISTA");
    private static final Set<String> CUSTOMER = Set.of("CUSTOMER");
    private record Access(String method, String path, Set<String> roles) {}

    static Stream<Arguments> permissions() {
        List<Access> cases = new ArrayList<>();
        for (String resource : List.of("admins", "users")) {
            cases.add(new Access("GET", "/api/admin/" + resource, SUPER_ADMIN));
            cases.add(new Access("GET", "/api/admin/" + resource + "/record", SUPER_ADMIN));
            cases.add(new Access("PATCH", "/api/admin/" + resource + "/record", SUPER_ADMIN));
            cases.add(new Access("DELETE", "/api/admin/" + resource + "/record", SUPER_ADMIN));
        }
        cases.add(new Access("POST", "/api/admin/admins", SUPER_ADMIN));
        cases.add(new Access("PATCH", "/api/admin/users/record/status", SUPER_ADMIN));
        for (String resource : List.of("baristas", "categories", "products", "events", "banners", "expenses")) {
            Set<String> readers = Set.of("categories", "products").contains(resource) ? STAFF : MANAGERS;
            cases.add(new Access("GET", "/api/admin/" + resource, readers));
            cases.add(new Access("GET", "/api/admin/" + resource + "/record", readers));
            cases.add(new Access("POST", "/api/admin/" + resource, MANAGERS));
            cases.add(new Access("PATCH", "/api/admin/" + resource + "/record", MANAGERS));
            cases.add(new Access("DELETE", "/api/admin/" + resource + "/record", MANAGERS));
        }
        for (String path : List.of("inventory", "inventory/record", "inventory/low-stock", "inventory/record/movements", "products/record/size-options")) {
            cases.add(new Access("GET", "/api/admin/" + path, STAFF));
        }
        for (String path : List.of("inventory/stock-in", "inventory/stock-cut", "products/import", "products/record/image", "products/record/size-options", "attendance", "orders/record/collect-cash", "orders/record/complete")) {
            cases.add(new Access("POST", "/api/admin/" + path, MANAGERS));
        }
        for (String path : List.of("attendance", "attendance/record/history", "orders", "orders/record", "reports/daily", "finance/daily", "finance/monthly", "finance/yearly", "bakong/exchange-rate")) {
            cases.add(new Access("GET", "/api/admin/" + path, MANAGERS));
        }
        cases.add(new Access("PATCH", "/api/admin/attendance/record", MANAGERS));
        cases.add(new Access("PUT", "/api/admin/products/record/discount", MANAGERS));
        cases.add(new Access("DELETE", "/api/admin/products/record/discount", MANAGERS));
        cases.add(new Access("PUT", "/api/admin/bakong/exchange-rate", MANAGERS));
        for (String path : List.of("orders", "orders/all", "reports/daily", "attendance", "attendance/current")) {
            cases.add(new Access("GET", "/api/barista/" + path, BARISTA));
        }
        for (String path : List.of("orders", "orders/record/pay/cash", "orders/record/complete", "attendance/check-in", "attendance/check-out")) {
            cases.add(new Access("POST", "/api/barista/" + path, BARISTA));
        }
        cases.add(new Access("GET", "/api/customer/cart", CUSTOMER));
        cases.add(new Access("GET", "/api/customer/orders", CUSTOMER));
        cases.add(new Access("POST", "/api/customer/cart/checkout", CUSTOMER));
        for (String path : List.of("/api/admin/unlisted", "/api/barista/unlisted", "/api/barista/reports/daily")) {
            cases.add(new Access("POST", path, Set.of()));
        }
        return cases.stream().flatMap(access -> Stream.of("SUPER_ADMIN", "ADMIN", "BARISTA", "CUSTOMER", "ANONYMOUS")
                .map(role -> Arguments.of(access.method, access.path, role,
                        role.equals("ANONYMOUS") ? 401 : access.roles.contains(role) ? 200 : 403)));
    }

    @ParameterizedTest(name = "{2} {0} {1} -> {3}")
    @MethodSource("permissions")
    void enforcesRoleMatrix(String method, String path, String role, int expectedStatus) throws Exception {
        var call = request(HttpMethod.valueOf(method), path);
        if (!role.equals("ANONYMOUS")) call.with(user("staff@example.test").roles(role));
        mvc.perform(call).andExpect(status().is(expectedStatus));
    }

    static Stream<String> publicReads() {
        return Stream.of("/api/customer/products", "/api/customer/products/record", "/api/banners", "/api/events");
    }

    @ParameterizedTest
    @MethodSource("publicReads")
    void preservesPublicStorefrontReads(String path) throws Exception {
        mvc.perform(request(HttpMethod.GET, path)).andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {
        @RequestMapping("/api/**")
        String handle() { return "allowed"; }
    }
}
