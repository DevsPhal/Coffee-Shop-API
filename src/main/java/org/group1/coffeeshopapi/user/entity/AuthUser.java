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

/**
 * A role-indexed index row for every account in the system — every {@code Admin}/{@code Barista}/
 * {@code Customer}, plus the config-driven Super Admin (see
 * {@code AuthUserSyncService#syncSuperAdmin()}), which has no row of its own anywhere else. Not
 * part of the {@code User}/{@code Admin}/{@code Barista}/{@code Customer} JPA hierarchy — those
 * are each their own standalone table under {@code TABLE_PER_CLASS} (see {@link User}) — so this
 * row is written explicitly by {@code AuthUserSyncService} whenever one of those is created or
 * changes, rather than being derived by Hibernate.
 * <p>
 * {@code name} and {@code status} are a deliberate denormalized copy — kept in sync by
 * {@code AuthUserSyncService} on every write to the source account — so a caller that doesn't know
 * (or care about) the role ahead of time can list/search every account in one place without
 * joining {@code admins}/{@code baristas}/{@code customers}, and without special-casing the Super
 * Admin, which has no such table to join.
 * <p>
 * {@code id} always matches the source row's id (every concrete {@code User} subtype shares
 * {@code BaseEntity}'s UUID generation), so for a staff/customer account exactly one of
 * {@link #admin}, {@link #barista}, {@link #customer} resolves — whichever one {@link #role} says
 * it is — and the other two simply find no matching row in their table; for the Super Admin all
 * three resolve to nothing, which is expected.
 */
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

    // Nullable despite always being set by AuthUserSyncService going forward: this project has no
    // migration tool (ddl-auto: update only, see NoEnumCheckPostgreSQLDialect), and a NOT NULL
    // column added to an already-populated table fails outright. Any pre-existing row simply
    // reads as null here until its next sync (see AuthUserSyncService.sync).
    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status;

    // Not FetchType.LAZY: an optional shared-primary-key one-to-one like these can't actually be
    // lazy without bytecode enhancement (Hibernate has to query to know whether a proxy should
    // even exist) — declaring LAZY here would be silently ignored, so this is honest about it
    // instead. Whichever of the three doesn't match `role` costs one query that finds no row.
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
