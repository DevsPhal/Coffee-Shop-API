package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

import java.util.UUID;

// One row per account (admin, barista, customer, or the super admin), so any caller can list or
// search every account in one place without knowing the role ahead of time.
@Getter
@Setter
@Entity
@Table(name = "auth_users")
public class AuthUser {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 31)
    private Role role;

    // Nullable so adding this column doesn't break existing rows — it fills in on the next sync.
    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status;

    // Only the one matching `role` actually resolves — the other two find no row.
    @OneToOne
    @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    private Admin admin;

    @OneToOne
    @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    private Barista barista;

    @OneToOne
    @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    private Customer customer;
}
