package org.group1.coffeeshopapi.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
    Page<ContactMessage> findByStatus(ContactMessage.Status status, Pageable pageable);
    java.util.Optional<ContactMessage> findFirstByOrderIdAndStatus(UUID orderId, ContactMessage.Status status);
}
