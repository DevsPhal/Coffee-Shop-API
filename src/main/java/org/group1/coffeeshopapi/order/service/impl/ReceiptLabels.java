package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The fixed wording printed on a receipt, in the language staff picked. Only these words change
 * between languages: names, amounts and dates come from the same order either way. The Khmer terms
 * match the ones the admin dashboard already uses.
 */
final class ReceiptLabels {

    private static final DateTimeFormatter ENGLISH_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_24H = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] KHMER_MONTHS = {
            "មករា", "កុម្ភៈ", "មីនា", "មេសា", "ឧសភា", "មិថុនា",
            "កក្កដា", "សីហា", "កញ្ញា", "តុលា", "វិច្ឆិកា", "ធ្នូ"
    };

    private final ReceiptLanguage language;

    ReceiptLabels(ReceiptLanguage language) {
        this.language = language;
    }

    boolean khmer() {
        return language == ReceiptLanguage.KH;
    }

    String orderNumber(String code) {
        return pick("Order #", "លេខបញ្ជាទិញ៖ #") + code;
    }

    String date(LocalDateTime at) {
        String value;
        if (at == null) {
            value = "-";
        } else if (khmer()) {
            value = at.getDayOfMonth() + " " + KHMER_MONTHS[at.getMonthValue() - 1] + " " + at.getYear()
                    + " ម៉ោង " + at.format(TIME_24H);
        } else {
            value = at.format(ENGLISH_DATE);
        }
        return field("Date", "កាលបរិច្ឆេទ") + value;
    }

    String servedBy() {
        return field("Served By", "បម្រើដោយ");
    }

    String customer() {
        return field("Customer", "អតិថិជន");
    }

    String walkInCustomer() {
        return pick("Walk-in Customer", "អតិថិជនទូទៅ");
    }

    String subtotal() {
        return pick("Subtotal", "សរុបរង");
    }

    String deliveryFee() {
        return pick("Delivery Fee", "ថ្លៃដឹកជញ្ជូន");
    }

    String total() {
        return pick("TOTAL", "សរុប");
    }

    String paymentMethod(PaymentMethod method) {
        String value = method == null ? "-" : switch (method) {
            case CASH -> pick("Cash", "សាច់ប្រាក់");
            case BAKONG -> "Bakong KHQR";
        };
        return field("Payment Method", "វិធីបង់ប្រាក់") + value;
    }

    String tendered() {
        return pick("Tendered", "ប្រាក់ទទួល");
    }

    String change() {
        return pick("Change", "ប្រាក់អាប់");
    }

    String note() {
        return field("Note", "កំណត់ចំណាំ");
    }

    String thanks() {
        return pick("Thank you, come again!", "សូមអរគុណ សូមអញ្ជើញមកម្តងទៀត");
    }

    String role(Role role) {
        return pick(readable(role.name()), switch (role) {
            case SUPER_ADMIN -> "អ្នកគ្រប់គ្រងជាន់ខ្ពស់";
            case ADMIN -> "អ្នកគ្រប់គ្រង";
            case BARISTA -> "បារីស្តា";
            case CUSTOMER -> "អតិថិជន";
        });
    }

    String variant(VariantLabel variant) {
        return pick(readable(variant.name()), switch (variant) {
            case MEDIUM -> "មធ្យម";
            case LARGE -> "ធំ";
            case PIECE -> "ដុំ";
            case PACK -> "កញ្ចប់";
            case BOX -> "ប្រអប់";
            case CARTON -> "កេស";
        });
    }

    String sugar(SugarLevel level) {
        return pick(readable(level.name()) + " sugar", switch (level) {
            case ZERO -> "គ្មានស្ករ";
            case LESS -> "ស្ករតិច";
            case NORMAL -> "ស្ករធម្មតា";
            case EXTRA -> "ស្ករច្រើន";
        });
    }

    String ice(IceLevel level) {
        return pick(readable(level.name()), switch (level) {
            case NO_ICE -> "គ្មានទឹកកក";
            case LESS_ICE -> "ទឹកកកតិច";
            case NORMAL -> "ទឹកកកធម្មតា";
            case EXTRA_ICE -> "ទឹកកកច្រើន";
        });
    }

    String milk(MilkType type) {
        return pick(readable(type.name()) + " milk", switch (type) {
            case NONE -> "គ្មានទឹកដោះគោ";
            case LESS -> "ទឹកដោះគោតិច";
            case NORMAL -> "ទឹកដោះគោធម្មតា";
            case EXTRA -> "ទឹកដោះគោច្រើន";
        });
    }

    private String pick(String english, String khmer) {
        return khmer() ? khmer : english;
    }

    // A label followed by its value; Khmer writes the colon as "៖".
    private String field(String english, String khmer) {
        return khmer() ? khmer + "៖ " : english + ": ";
    }

    private static String readable(String enumName) {
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
