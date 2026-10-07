package org.group1.coffeeshopapi.product.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ProductSkuGenerator {

    private static final Pattern SKU_PATTERN = Pattern.compile(ValidationPatterns.SKU_REGEX);
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("\\d+");
    private static final int CATEGORY_CODE_LENGTH = 3;
    private static final int NAME_CODE_LENGTH = 3;
    private static final int MAX_SEQUENCE = 999;

    private final ProductRepository productRepository;

    public String generate(Category category, String productName) {
        return generate(category, productName, Set.of());
    }

    public String generate(Category category, String productName, Set<String> reserved) {
        String prefix = prefix(category, productName);
        int next = productRepository.findSkusStartingWith(prefix).stream()
                .mapToInt(sku -> sequenceOf(sku, prefix))
                .max()
                .orElse(0) + 1;
        for (int sequence = next; sequence <= MAX_SEQUENCE; sequence++) {
            String candidate = prefix + String.format("%03d", sequence);
            if (!reserved.contains(candidate) && !productRepository.existsBySkuIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new InvalidOperationException("No free SKU left for prefix " + prefix + " — enter one manually");
    }

    public String regenerate(Product product) {
        String prefix = prefix(product.getCategory(), product.getName());
        if (product.getSku() != null && sequenceOf(product.getSku(), prefix) > 0) {
            return product.getSku().toUpperCase(Locale.ROOT);
        }
        return generate(product.getCategory(), product.getName());
    }

    public String prefix(Category category, String productName) {
        return categoryPrefix(category) + code(productName, NAME_CODE_LENGTH, "PRD") + "-";
    }

    public String categoryPrefix(Category category) {
        return groupCode(category.getCategoryGroup()) + "-"
                + code(category.getName(), CATEGORY_CODE_LENGTH, "GEN") + "-";
    }

    public static String variantSku(String productSku, VariantLabel label) {
        if (productSku == null || label == null) {
            return null;
        }
        return productSku + "-" + label.getSkuSuffix();
    }

    public static String normalizeManual(String raw) {
        String sku = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (sku.isEmpty()) {
            throw new InvalidOperationException("SKU is required when SKU mode is MANUAL");
        }
        if (sku.length() > ValidationPatterns.SKU_MAX_LENGTH) {
            throw new InvalidOperationException(
                    "SKU must not exceed " + ValidationPatterns.SKU_MAX_LENGTH + " characters");
        }
        if (!SKU_PATTERN.matcher(sku).matches()) {
            throw new InvalidOperationException(ValidationPatterns.SKU_MESSAGE);
        }
        return sku;
    }

    private static String groupCode(CategoryGroup group) {
        if (group == null) {
            return "GN";
        }
        return switch (group) {
            case FRESH_DRINK -> "FD";
            case BEVERAGE -> "BV";
            case SNACK -> "SN";
        };
    }

    private static String code(String text, int length, String fallback) {
        String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        String[] words = Arrays.stream(ascii.split("[^A-Z0-9]+"))
                .filter(word -> !word.isEmpty())
                .toArray(String[]::new);
        if (words.length == 0) {
            return fallback;
        }
        if (words.length == 1) {
            return words[0].substring(0, Math.min(length, words[0].length()));
        }
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < words.length && initials.length() < length; i++) {
            initials.append(words[i].charAt(0));
        }
        return initials.toString();
    }

    private static int sequenceOf(String sku, String prefix) {
        String upper = sku.toUpperCase(Locale.ROOT);
        if (!upper.startsWith(prefix)) {
            return 0;
        }
        String rest = upper.substring(prefix.length());
        if (!SEQUENCE_PATTERN.matcher(rest).matches() || rest.length() > 6) {
            return 0;
        }
        return Integer.parseInt(rest);
    }
}
