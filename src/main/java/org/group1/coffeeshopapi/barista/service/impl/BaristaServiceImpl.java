package org.group1.coffeeshopapi.barista.service.impl;

import java.util.UUID;

import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.barista.mapper.BaristaMapper;
import org.group1.coffeeshopapi.barista.service.BaristaService;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class BaristaServiceImpl implements BaristaService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BaristaMapper baristaMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BaristaResponse> getAllBaristas(Pageable pageable) {
        Page<BaristaResponse> page = userRepository.findByRole(Role.BARISTA, pageable).map(baristaMapper::toResponse);
        return PageUtil.toPaginatedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public BaristaResponse getBaristaById(UUID id) {
        User user = findBarista(id);
        return baristaMapper.toResponse(user);
    }

    @Override
    public BaristaResponse createBarista(BaristaRequest request) {
        validateUniqueBarista(request, null);
        log.info("Barista before saving: {}", request);
        User user = baristaMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword() == null ? "" : request.getPassword()));
        user.setRole(Role.BARISTA);
        user.setEnabled(request.getEnabled() == null ? true : request.getEnabled());
        User saved = userRepository.save(user);
        log.info("Barista after saving: {}", saved.getId());
        return baristaMapper.toResponse(saved);
    }

    @Override
    public BaristaResponse updateBarista(UUID id, BaristaRequest request) {
        User user = findBarista(id);
        validateUniqueBarista(request, id);
        baristaMapper.updateEntity(request, user);

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return baristaMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteBarista(UUID id) {
        userRepository.delete(findBarista(id));
    }

    private User findBarista(UUID id) {
        return userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.BARISTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barista not found: " + id));
    }

    private void validateUniqueBarista(BaristaRequest request, UUID currentId) {
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && (currentId == null
                ? userRepository.existsByPhoneNumber(request.getPhone())
                : userRepository.existsByPhoneNumberAndIdNot(request.getPhone(), currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && (currentId == null
                ? userRepository.existsByEmail(request.getEmail())
                : userRepository.existsByEmailAndIdNot(request.getEmail(), currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && (currentId == null
                ? userRepository.existsByUsername(request.getUsername())
                : userRepository.existsByUsernameAndIdNot(request.getUsername(), currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
    }
}
