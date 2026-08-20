package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.group1.coffeeshopapi.common.enums.Role;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customers_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_customers_phone_number", columnNames = "phone_number"),
        @UniqueConstraint(name = "uk_customers_telegram_chat_id", columnNames = "telegram_chat_id")
})
public class Customer extends User {

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }
}