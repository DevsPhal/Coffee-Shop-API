package org.group1.coffeeshopapi.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
public class ContactMessage extends BaseEntity {
    public enum Status { RECEIVED, RESOLVED }

    @Column
    private java.util.UUID orderId;

    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(length = 30)
    private String phone;
    @Column(nullable = false, length = 100)
    private String topic;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RECEIVED;
}
