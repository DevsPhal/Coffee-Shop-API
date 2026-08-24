package org.group1.coffeeshopapi.barista.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.entity.User;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "baristas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_baristas_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_baristas_phone_number", columnNames = "phone_number"),
        @UniqueConstraint(name = "uk_baristas_telegram_chat_id", columnNames = "telegram_chat_id")
})
public class Barista extends User {

    // Which admin/super admin created this account — null for pre-existing rows.
    @Column
    private UUID createdBy;

    @Override
    public Role getRole() {
        return Role.BARISTA;
    }
}