package org.group1.coffeeshopapi.barista.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.barista.mapper.BaristaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BaristaServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BaristaMapper baristaMapper;

    @InjectMocks
    private BaristaServiceImpl baristaService;

    @Test
    void createBarista_shouldSaveMappedUser() {
        BaristaRequest request = BaristaRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .phone("012345678")
                .password("secret")
                .enabled(true)
                .build();

        User user = User.builder()
                .fullName("Alice")
                .email("alice@example.com")
                .phoneNumber("012345678")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        BaristaResponse response = BaristaResponse.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .build();

        when(baristaMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(baristaMapper.toResponse(user)).thenReturn(response);

        BaristaResponse result = baristaService.createBarista(request);

        assertNotNull(result);
        verify(passwordEncoder).encode("secret");
        verify(userRepository).save(any(User.class));
    }
}
