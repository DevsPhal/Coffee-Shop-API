package org.group1.coffeeshopapi.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.entity.User;
import org.hibernate.annotations.BatchSize;

import java.util.UUID;

// BatchSize: several entities (Product, Category, Event, Banner, ...) track "which admin did
// this" as a lazy @ManyToOne here — this batches those proxy initializations into one IN-clause
// query per distinct group of admins referenced on a page, instead of one query per row.
@Getter
@Setter
@Entity
@BatchSize(size = 20)
@Table(name = "admins", uniqueConstraints = {
        @UniqueConstraint(name = "uk_admins_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_admins_phone_number", columnNames = "phone_number"),
        @UniqueConstraint(name = "uk_admins_telegram_chat_id", columnNames = "telegram_chat_id")
})
public class Admin extends User {

    // Which admin/super admin created this account — null for pre-existing rows.
    @Column
    private UUID createdBy;

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}