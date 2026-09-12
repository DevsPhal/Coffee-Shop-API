package org.group1.coffeeshopapi.contact;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContactMessageController {
    private final ContactMessageService messages;

    public record StatusRequest(@NotNull ContactMessage.Status status) {}

    @PostMapping("/api/contact-messages")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submit(@Valid @RequestBody ContactMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Message received", messages.submit(request)));
    }

    @GetMapping("/api/admin/contact-messages")
    public ApiResponse<PageResponse<ContactMessageResponse>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) ContactMessage.Status status) {
        return ApiResponse.of(HttpStatus.OK, "Success",
                PageResponse.of(messages.list(status, PageUtil.buildPageable(page, size))));
    }

    @PatchMapping("/api/admin/contact-messages/{id}")
    public ApiResponse<ContactMessageResponse> updateStatus(@PathVariable UUID id,
            @Valid @RequestBody StatusRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Message updated", messages.updateStatus(id, request.status()));
    }
}
