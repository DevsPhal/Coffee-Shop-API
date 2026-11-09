package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.OrderType;
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
 * between languages: names, amounts and dates come from the same order either way. The Khmer terms,
 * and the English drink options, match the ones the admin dashboard already uses.
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

    String title(boolean invoice) {
        return invoice ? pick("INVOICE", "វិក្កយបត្រ") : pick("RECEIPT", "បង្កាន់ដៃ");
    }

    String orderNumber() {
        return pick("Order No.", "លេខបញ្ជាទិញ");
    }

    String date() {
        return pick("Date", "កាលបរិច្ឆេទ");
    }

    String formatDate(LocalDateTime at) {
        if (khmer()) {
            return at.getDayOfMonth() + " " + KHMER_MONTHS[at.getMonthValue() - 1] + " " + at.getYear()
                    + " ម៉ោង " + at.format(TIME_24H);
        }
        return at.format(ENGLISH_DATE);
    }

    String servedBy() {
        return pick("Served by", "បម្រើដោយ");
    }

    String table() {
        return pick("Table", "តុ");
    }

    String orderType() {
        return pick("Order type", "ប្រភេទការកុម្ម៉ង់");
    }

    String orderType(OrderType type) {
        return switch (type) {
            case DINE_IN -> pick("Dine in", "ញ៉ាំនៅហាង");
            case TAKE_AWAY -> pick("Take away", "ខ្ចប់យកទៅ");
        };
    }

    String customer() {
        return pick("Customer", "អតិថិជន");
    }

    String walkInCustomer() {
        return pick("Walk-in Customer", "អតិថិជនទូទៅ");
    }

    String phone() {
        return pick("Phone", "លេខទូរស័ព្ទ");
    }

    String address() {
        return pick("Address", "អាសយដ្ឋាន");
    }

    String subtotal() {
        return pick("Subtotal", "សរុបរង");
    }

    String deliveryFee() {
        return pick("Delivery fee", "ថ្លៃដឹកជញ្ជូន");
    }

    String total() {
        return pick("TOTAL", "សរុប");
    }

    String paymentMethod() {
        return pick("Payment method", "វិធីបង់ប្រាក់");
    }

    String payment(PaymentMethod method) {
        return switch (method) {
            case CASH -> pick("Cash", "សាច់ប្រាក់");
            case BAKONG -> "Bakong KHQR";
        };
    }

    String amountReceived() {
        return pick("Amount received", "ប្រាក់ទទួល");
    }

    String amountPaid() {
        return pick("Amount paid", "ប្រាក់បានបង់");
    }

    String change() {
        return pick("Change", "ប្រាក់អាប់");
    }

    String totalKhr() {
        return pick("Total in KHR", "សរុបជារៀល");
    }

    String exchangeRate() {
        return pick("Exchange rate", "អត្រាប្តូរប្រាក់");
    }

    String note() {
        return pick("Note", "កំណត់ចំណាំ");
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
        return switch (level) {
            case ZERO -> pick("No Sugar", "គ្មានស្ករ");
            case LESS -> pick("Less Sugar", "ស្ករតិច");
            case NORMAL -> pick("Normal Sugar", "ស្ករធម្មតា");
            case EXTRA -> pick("Extra Sugar", "ស្ករច្រើន");
        };
    }

    String ice(IceLevel level) {
        return switch (level) {
            case NO_ICE -> pick("No Ice", "គ្មានទឹកកក");
            case LESS_ICE -> pick("Less Ice", "ទឹកកកតិច");
            case NORMAL -> pick("Normal Ice", "ទឹកកកធម្មតា");
            case EXTRA_ICE -> pick("Extra Ice", "ទឹកកកច្រើន");
        };
    }

    String milk(MilkType type) {
        return switch (type) {
            case NONE -> pick("No Milk", "គ្មានទឹកដោះគោ");
            case LESS -> pick("Less Milk", "ទឹកដោះគោតិច");
            case NORMAL -> pick("Normal Milk", "ទឹកដោះគោធម្មតា");
            case EXTRA -> pick("Extra Milk", "ទឹកដោះគោច្រើន");
        };
    }

    private String pick(String english, String khmer) {
        return khmer() ? khmer : english;
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
