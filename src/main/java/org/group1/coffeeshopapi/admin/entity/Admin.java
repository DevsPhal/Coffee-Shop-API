package org.group1.coffeeshopapi.admin.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.entity.User;

@Entity
@Table(name = "admins")
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}