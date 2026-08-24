package org.group1.coffeeshopapi.common.enums;

// The only two currencies Bakong KHQR accepts for this shop. All prices are stored in USD;
// KHR only ever appears as the currency a QR was encoded/paid in (see Order.bakongCurrency).
public enum Currency {
    USD, KHR
}
