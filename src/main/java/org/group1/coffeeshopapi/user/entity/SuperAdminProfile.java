package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Gender;

import java.util.UUID;

/**
 * The editable details of the config-driven super admin, which has no users row. There is only ever
 * one row, keyed by SuperAdminUserDetails.ID; sign-in email and password stay in the deployment config.
 */
@Getter
@Setter
@Entity
@Table(name = "super_admin_profile")
public class SuperAdminProfile {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column
    private String phoneNumber;

    @Column
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;
}
