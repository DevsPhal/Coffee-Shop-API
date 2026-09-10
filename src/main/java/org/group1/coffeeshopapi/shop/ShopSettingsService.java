package org.group1.coffeeshopapi.shop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ShopSettingsService {
    private final BigDecimal deliveryFee;

    public ShopSettingsService(@Value("${app.orders.delivery-fee:0.50}") BigDecimal deliveryFee) {
        if (deliveryFee.signum() < 0) throw new IllegalArgumentException("Delivery fee cannot be negative");
        this.deliveryFee = deliveryFee.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal deliveryFee() {
        return deliveryFee;
    }
}
