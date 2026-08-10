package org.group1.coffeeshopapi.barista.service.impl;

import java.util.List;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.admin.repository.UserRepository;
import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.barista.mapper.BaristaMapper;
import org.group1.coffeeshopapi.barista.service.BaristaService;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BaristaServiceImpl implements BaristaService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BaristaMapper baristaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BaristaResponse> getAllBaristas() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.BARISTA)
                .map(baristaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BaristaResponse getBaristaById(Long id) {
        User user = findBarista(id);
        return baristaMapper.toResponse(user);
    }

    @Override
    public BaristaResponse createBarista(BaristaRequest request) {
        User user = baristaMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword() == null ? "" : request.getPassword()));
        user.setRole(Role.BARISTA);
        user.setEnabled(request.getEnabled() == null ? true : request.getEnabled());
        return baristaMapper.toResponse(userRepository.save(user));
    }

    @Override
    public BaristaResponse updateBarista(Long id, BaristaRequest request) {
        User user = findBarista(id);
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
    public void deleteBarista(Long id) {
        userRepository.delete(findBarista(id));
    }

    private User findBarista(Long id) {
        return userRepository.findById(id)
                .filter(candidate -> candidate.getRole() == Role.BARISTA)
                .orElseThrow(() -> new ResourceNotFoundException("Barista not found: " + id));
    }
}
