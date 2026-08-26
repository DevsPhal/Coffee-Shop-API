package org.group1.coffeeshopapi.bakong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Single-row table holding the live USD-to-KHR rate admins can update without a redeploy — unlike
 * the rest of Bakong's merchant config in {@code BakongProperties} (account, token, labels), the
 * exchange rate moves with the real market and needs to be changeable at runtime.
 */
@Getter
@Setter
@Entity
@Table(name = "bakong_exchange_rate")
@EntityListeners(AuditingEntityListener.class)
public class BakongExchangeRate {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id = SINGLETON_ID;

    // What's actually used to convert order totals into KHR for the Bakong QR — admin's own
    // working rate, which may deliberately differ from marketRate below.
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal khrPerUsdRate;

    // The real-world market rate, kept purely as a reference point for admins to compare
    // khrPerUsdRate against — entered by hand alongside it, never itself used for QR conversion.
    @Column(precision = 15, scale = 4)
    private BigDecimal marketRate;

    @Column
    private UUID updatedByAdminId;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
