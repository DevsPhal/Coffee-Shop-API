package org.group1.coffeeshopapi.barista.controller;

import java.util.UUID;

import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.barista.service.BaristaService;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/barista")
@PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
@RequiredArgsConstructor
public class BaristaController {

    private final BaristaService baristaService;

    @GetMapping
    public PaginatedResponse<BaristaResponse> getAllBaristas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return baristaService.getAllBaristas(pageable);
    }

    @GetMapping("/{id}")
    public BaristaResponse getBaristaById(@PathVariable UUID id) {
        return baristaService.getBaristaById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BaristaResponse createBarista(@RequestBody BaristaRequest request) {
        return baristaService.createBarista(request);
    }

    @PutMapping("/{id}")
    public BaristaResponse updateBarista(@PathVariable UUID id, @RequestBody BaristaRequest request) {
        return baristaService.updateBarista(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteBarista(@PathVariable UUID id) {
        baristaService.deleteBarista(id);
    }
}
