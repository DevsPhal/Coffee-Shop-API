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

import java.util.UUID;

/**
 * A thin, role-indexed pointer at whichever of {@code admins}/{@code baristas}/{@code customers}
 * actually owns this account — kept in {@code auth_users} so a caller that doesn't know the role
 * ahead of time can still find/list every account across all three in one place, without
 * duplicating any of their columns. Not part of the {@code User}/{@code Admin}/{@code Barista}/
 * {@code Customer} JPA hierarchy — those are each their own standalone table under
 * {@code TABLE_PER_CLASS} (see {@link User}) — so this row is written explicitly by
 * {@code AuthUserSyncService} whenever one of those is created, rather than being derived by
 * Hibernate.
 * <p>
 * {@code id} always matches the source row's id (every concrete {@code User} subtype shares
 * {@code BaseEntity}'s UUID generation), so exactly one of {@link #admin}, {@link #barista},
 * {@link #customer} resolves — whichever one {@link #role} says it is — and the other two simply
 * find no matching row in their table. Immutable once created: {@code id} and {@code role} never
 * change for the lifetime of an account, so unlike the mirror this replaced, there's nothing here
 * that can go stale on an update — the real, current data always lives on the associated entity
 * itself.
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
