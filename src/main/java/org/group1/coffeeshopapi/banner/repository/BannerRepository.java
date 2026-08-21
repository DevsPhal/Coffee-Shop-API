package org.group1.coffeeshopapi.banner.repository;

import org.group1.coffeeshopapi.banner.entity.Banner;
import org.group1.coffeeshopapi.common.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {
    Page<Banner> findAllByOrderBySortOrderAsc(Pageable pageable);
    List<Banner> findByStatusOrderBySortOrderAsc(Status status);
}
