package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramCatalogService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramCatalogServiceImpl implements TelegramCatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    public String buildMenu(String categoryName) {
        if (categoryName != null && !categoryName.isBlank()) {
            return buildMenuForCategory(categoryName.trim());
        }

        List<Product> products = productRepository.findByStatusOrderByNameAsc(Status.ACTIVE);
        if (products.isEmpty()) {
            return "🛒 The menu is empty right now — please check back soon!";
        }

        // Case-insensitive TreeMap for alphabetical category order (matches the DB collation used
        // by /categories); within each group, products stay in the name-sorted order they were
        // fetched in.
        Map<String, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().getName(),
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Collectors.toList()));

        Map<UUID, List<ProductVariant>> variantsByProduct = fetchActiveVariants(products);
        StringBuilder sb = new StringBuilder("🛒 <b>Menu</b>\n");
        for (Map.Entry<String, List<Product>> entry : byCategory.entrySet()) {
            sb.append("\n<b>").append(TelegramFormat.escape(TelegramFormat.titleCase(entry.getKey()))).append("</b>\n");
            entry.getValue().forEach(p -> appendProductLine(sb, p, variantsByProduct.getOrDefault(p.getId(), List.of())));
        }
        sb.append("\nSend /menu &lt;category&gt; to filter, or /discounts for today's deals.");
        return sb.toString();
    }

    @Override
    public String buildCategoryList() {
        List<Category> categories = categoryRepository.findByStatusOrderByNameAsc(Status.ACTIVE);
        if (categories.isEmpty()) {
            return "🗂 No categories available right now.";
        }

        StringBuilder sb = new StringBuilder("🗂 <b>Categories</b>\n\n");
        categories.forEach(c -> sb.append("• ").append(TelegramFormat.escape(TelegramFormat.titleCase(c.getName()))).append('\n'));
        sb.append("\nSend /menu &lt;category name&gt; to view items, or /menu to see everything.");
        return sb.toString();
    }

    @Override
    public String buildDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> discounted = productRepository.findByStatusOrderByNameAsc(Status.ACTIVE).stream()
                .filter(p -> p.isDiscountActive(now))
                .toList();
        if (discounted.isEmpty()) {
            return "🔥 No active discounts right now — check back soon!";
        }

        Map<UUID, List<ProductVariant>> variantsByProduct = fetchActiveVariants(discounted);
        StringBuilder sb = new StringBuilder("🔥 <b>Today's Deals</b>\n\n");
        discounted.forEach(p -> appendProductLine(sb, p, variantsByProduct.getOrDefault(p.getId(), List.of())));
        return sb.toString();
    }

    private String buildMenuForCategory(String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
        if (category == null || category.getStatus() != Status.ACTIVE) {
            return "🛒 No category named \"" + TelegramFormat.escape(categoryName) + "\". Send /categories to see what's available.";
        }

        List<Product> products = productRepository.findByCategoryIdAndStatusOrderByNameAsc(category.getId(), Status.ACTIVE);
        String displayCategoryName = TelegramFormat.escape(TelegramFormat.titleCase(category.getName()));
        if (products.isEmpty()) {
            return "🛒 No items available in <b>" + displayCategoryName + "</b> right now.";
        }

        Map<UUID, List<ProductVariant>> variantsByProduct = fetchActiveVariants(products);
        StringBuilder sb = new StringBuilder("🛒 <b>").append(displayCategoryName).append("</b>\n\n");
        products.forEach(p -> appendProductLine(sb, p, variantsByProduct.getOrDefault(p.getId(), List.of())));
        return sb.toString();
    }

    private Map<UUID, List<ProductVariant>> fetchActiveVariants(List<Product> products) {
        List<UUID> productIds = products.stream().map(Product::getId).toList();
        Map<UUID, List<ProductVariant>> byProduct = new HashMap<>();
        for (ProductVariant variant : variantRepository
                .findByProductIdInAndStatusOrderBySortOrderAscNameAsc(productIds, Status.ACTIVE)) {
            byProduct.computeIfAbsent(variant.getProduct().getId(), id -> new ArrayList<>()).add(variant);
        }
        return byProduct;
    }

    // Shows the cheapest active variant's price, prefixed with "from" when the product has more
    // than one — a product with no priced variant yet is shown without a price.
    private void appendProductLine(StringBuilder sb, Product product, List<ProductVariant> variants) {
        LocalDateTime now = LocalDateTime.now();
        sb.append("• ").append(TelegramFormat.escape(TelegramFormat.titleCase(product.getName()))).append(" — ");
        if (variants.isEmpty()) {
            sb.append("price not set");
        } else {
            ProductVariant cheapest = variants.stream()
                    .min(Comparator.comparing(ProductVariant::getPrice))
                    .orElseThrow();
            String prefix = variants.size() > 1 ? "from " : "";
            BigDecimal price = cheapest.getPrice();
            if (product.isDiscountActive(now)) {
                sb.append(prefix).append("<s>").append(TelegramFormat.usd(price)).append("</s> <b>")
                        .append(TelegramFormat.usd(product.getFinalPrice(price, now))).append("</b> ")
                        .append(discountBadge(product));
            } else {
                sb.append(prefix).append(TelegramFormat.usd(price));
            }
        }
        sb.append('\n');
    }

    private String discountBadge(Product product) {
        return product.getDiscountType() == DiscountType.PERCENTAGE
                ? "(" + product.getDiscountValue().stripTrailingZeros().toPlainString() + "% OFF)"
                : "(" + TelegramFormat.usd(product.getDiscountValue()) + " OFF)";
    }
}
