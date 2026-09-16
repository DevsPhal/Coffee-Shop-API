package org.group1.coffeeshopapi.admin;

import org.group1.coffeeshopapi.admin.controller.AdminController;
import org.group1.coffeeshopapi.admin.controller.BaristaController;
import org.group1.coffeeshopapi.admin.service.StaffService;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.filter.JwtAuthFilter;
import org.group1.coffeeshopapi.common.properties.CorsProperties;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.security.CustomUserDetailsService;
import org.group1.coffeeshopapi.common.security.RestAccessDeniedHandler;
import org.group1.coffeeshopapi.common.security.RestAuthenticationEntryPoint;
import org.group1.coffeeshopapi.common.util.JwtUtil;
import org.group1.coffeeshopapi.config.SecurityConfig;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AdminController.class, BaristaController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, CorsProperties.class})
class StaffAvatarControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean StaffService staffService;
    @MockitoBean CurrentActor currentActor;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean StringRedisTemplate redisTemplate;

    @ParameterizedTest
    @CsvSource({
            "SUPER_ADMIN,admins,200", "ADMIN,admins,403", "BARISTA,admins,403", "CUSTOMER,admins,403", "ANONYMOUS,admins,401",
            "SUPER_ADMIN,baristas,200", "ADMIN,baristas,200", "BARISTA,baristas,403", "CUSTOMER,baristas,403", "ANONYMOUS,baristas,401"
    })
    void uploadUsesStaffManagementPermissions(String caller, String resource, int expected) throws Exception {
        UUID id = UUID.randomUUID();
        Role targetRole = resource.equals("admins") ? Role.ADMIN : Role.BARISTA;
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        if (expected == 200) {
            when(staffService.uploadAvatar(id, file, targetRole)).thenReturn(UserResponse.builder()
                    .id(id).role(targetRole).avatarUrl("https://images.example.test/avatars/photo.png").build());
        }
        var call = multipart("/api/admin/{resource}/{id}/avatar", resource, id).file(file);
        if (!caller.equals("ANONYMOUS")) call.with(user("staff@example.test").roles(caller));
        var result = mvc.perform(call).andExpect(status().is(expected));
        if (expected == 200) {
            result.andExpect(jsonPath("$.data.avatarUrl").value("https://images.example.test/avatars/photo.png"));
            verify(staffService).uploadAvatar(id, file, targetRole);
        } else {
            verifyNoInteractions(staffService);
        }
    }

    @Test
    void missingFileDoesNotModifyStaff() throws Exception {
        mvc.perform(multipart("/api/admin/baristas/{id}/avatar", UUID.randomUUID()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(staffService);
    }
}
