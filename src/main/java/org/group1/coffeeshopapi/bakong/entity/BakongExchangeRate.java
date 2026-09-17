package org.group1.coffeeshopapi.bakong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Single-row table holding the live USD-to-KHR rate — admins can update it without a redeploy.
@Getter
@Setter
@Entity
@Table(name = "bakong_exchange_rate")
@EntityListeners(AuditingEntityListener.class)
public class BakongExchangeRate {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id = SINGLETON_ID;

    // The rate actually used to convert order totals into KHR for the Bakong QR.
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal khrPerUsdRate;

    // The real-world market rate, kept only for admins to compare khrPerUsdRate against.
    @Column(precision = 15, scale = 4)
    private BigDecimal marketRate;

    // Null if never updated, or if the Super Admin made the last change (no admin row to reference).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_id")
    private Admin updatedByAdmin;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
