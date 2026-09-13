package org.group1.coffeeshopapi.contact;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactMessageService {
    private final ContactMessageRepository messages;
    private final org.group1.coffeeshopapi.order.repository.OrderRepository orders;

    @Transactional
    public ContactMessageResponse requestAssistance(UUID orderId, UUID customerId) {
        var order = orders.findByCustomerForUpdate(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        var existing = messages.findFirstByOrderIdAndStatus(orderId, ContactMessage.Status.RECEIVED);
        if (existing.isPresent()) return ContactMessageResponse.of(existing.get());
        var message = new ContactMessage();
        message.setOrderId(orderId);
        message.setFullName(order.getContactName() != null ? order.getContactName() : order.getCustomer().getFullName());
        message.setEmail(order.getCustomer().getEmail());
        message.setPhone(order.getContactPhone());
        message.setTopic("Order assistance");
        message.setMessage("Customer requested staff assistance for order " + orderId + ".");
        return ContactMessageResponse.of(messages.save(message));
    }

    @Transactional
    public ContactMessageResponse submit(ContactMessageRequest request) {
        var message = new ContactMessage();
        message.setFullName(request.fullName().trim());
        message.setEmail(request.email().trim());
        message.setPhone(request.phone() == null ? null : request.phone().trim());
        message.setTopic(request.topic().trim());
        message.setMessage(request.message().trim());
        return ContactMessageResponse.of(messages.save(message));
    }

    public Page<ContactMessageResponse> list(ContactMessage.Status status, Pageable pageable) {
        return (status == null ? messages.findAll(pageable) : messages.findByStatus(status, pageable))
                .map(ContactMessageResponse::of);
    }

    @Transactional
    public ContactMessageResponse updateStatus(UUID id, ContactMessage.Status status) {
        var message = messages.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
        message.setStatus(status);
        return ContactMessageResponse.of(messages.save(message));
    }
}
