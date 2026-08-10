package org.group1.coffeeshopapi.setting.repository;

import java.util.Optional;

import org.group1.coffeeshopapi.setting.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByKey(String key);
}
