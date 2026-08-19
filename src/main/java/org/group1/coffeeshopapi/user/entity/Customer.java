package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Role;

@Getter
@Setter
@Entity
@Table(name = "customers")
@DiscriminatorValue("CUSTOMER")
public class Customer extends User {

    @Column(unique = true)
    private String telegramChatId;

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }
}