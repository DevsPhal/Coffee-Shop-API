package org.group1.coffeeshopapi.barista.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.entity.User;

@Entity
@Table(name = "baristas")
@DiscriminatorValue("BARISTA")
public class Barista extends User {

    @Override
    public Role getRole() {
        return Role.BARISTA;
    }
}