package org.group1.coffeeshopapi.user.repository;

import org.group1.coffeeshopapi.user.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
}
