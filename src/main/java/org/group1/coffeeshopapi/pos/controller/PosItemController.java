package org.group1.coffeeshopapi.pos.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.pos.dto.request.PosItemRequest;
import org.group1.coffeeshopapi.pos.dto.response.PosItemResponse;
import org.group1.coffeeshopapi.pos.service.PosItemService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pos/items")
@PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
@RequiredArgsConstructor
public class PosItemController {

    private final PosItemService service;

    @GetMapping
    public List<PosItemResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/active")
    public List<PosItemResponse> getAllActive() {
        return service.getAllActive();
    }

    @GetMapping("/{id}")
    public PosItemResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PosItemResponse create(@RequestBody PosItemRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PosItemResponse update(@PathVariable UUID id, @RequestBody PosItemRequest request) {
        return service.update(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
