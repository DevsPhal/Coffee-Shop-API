package org.group1.coffeeshopapi.banner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Status;

// A promotional image shown on the storefront landing page, ordered by sortOrder ascending.
@Getter
@Setter
@Entity
@Table(name = "banners")
public class Banner extends BaseEntity {

    @Column
    private String title;

    @Column
    private String imageUrl;

    // Where tapping the banner should take the customer (a product/category path, or an external URL).
    @Column
    private String linkUrl;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
}
