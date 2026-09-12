package org.group1.coffeeshopapi.superadmin;

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
 * The editable half of the super admin account.
 * <p>
 * The account itself stays configuration-driven — {@code SUPER_ADMIN_EMAIL} and
 * {@code SUPER_ADMIN_PASSWORD} remain the only way in, so the shop can never be locked out of
 * its own admin by a bad database. What sits here is only the display profile: the name, phone,
 * gender and avatar shown around the app, which staff should be able to maintain themselves
 * rather than by editing a deployment file and restarting.
 * <p>
 * At most one row ever exists, pinned to {@link org.group1.coffeeshopapi.common.security.SuperAdminUserDetails#ID},
 * and its absence simply means "nothing has been customised yet".
 */
@Entity
@Table(name = "super_admin_profile")
@Getter
@Setter
public class SuperAdminProfile {

    @Id
    private UUID id;

    @Column(length = 100)
    private String fullName;

    @Column(length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 500)
    private String avatarUrl;
}
