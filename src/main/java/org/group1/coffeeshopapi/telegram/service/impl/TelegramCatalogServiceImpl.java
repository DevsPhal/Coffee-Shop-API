package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramCatalogService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramCatalogServiceImpl implements TelegramCatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public String buildMenu(String categoryName) {
        if (categoryName != null && !categoryName.isBlank()) {
            return buildMenuForCategory(categoryName.trim());
        }

        List<Product> products = productRepository.findByStatusOrderByNameAsc(Status.ACTIVE);
        if (products.isEmpty()) {
            return "The menu is empty right now. Please check back later.";
        }

        // Case-insensitive TreeMap for alphabetical category order (matches the DB collation used
        // by /categories); within each group, products stay in the name-sorted order they were
        // fetched in.
        Map<String, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().getName(),
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Collectors.toList()));

        StringBuilder sb = new StringBuilder("🛒 <b>Menu</b>\n");
        for (Map.Entry<String, List<Product>> entry : byCategory.entrySet()) {
            sb.append("\n<b>").append(TelegramFormat.escape(TelegramFormat.titleCase(entry.getKey()))).append("</b>\n");
            entry.getValue().forEach(p -> appendProductLine(sb, p));
        }
        sb.append("\nSend /menu &lt;category&gt; to filter, or /discounts for today's deals.");
        return sb.toString();
    }

    @Override
    public String buildCategoryList() {
        List<Category> categories = categoryRepository.findByStatusOrderByNameAsc(Status.ACTIVE);
        if (categories.isEmpty()) {
            return "No categories available right now.";
        }

        StringBuilder sb = new StringBuilder("<b>Categories</b>\n\n");
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
            return "No active discounts right now — check back soon!";
        }

        StringBuilder sb = new StringBuilder("🔥 <b>Today's Deals</b>\n\n");
        discounted.forEach(p -> appendProductLine(sb, p));
        return sb.toString();
    }

    private String buildMenuForCategory(String categoryName) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
        if (category == null || category.getStatus() != Status.ACTIVE) {
            return "No category named \"" + TelegramFormat.escape(categoryName) + "\". Send /categories to see what's available.";
        }

        List<Product> products = productRepository.findByCategoryIdAndStatusOrderByNameAsc(category.getId(), Status.ACTIVE);
        String displayCategoryName = TelegramFormat.escape(TelegramFormat.titleCase(category.getName()));
        if (products.isEmpty()) {
            return "No items available in <b>" + displayCategoryName + "</b> right now.";
        }

        StringBuilder sb = new StringBuilder("🛒 <b>").append(displayCategoryName).append("</b>\n\n");
        products.forEach(p -> appendProductLine(sb, p));
        return sb.toString();
    }

    private void appendProductLine(StringBuilder sb, Product product) {
        LocalDateTime now = LocalDateTime.now();
        sb.append("• ").append(TelegramFormat.escape(TelegramFormat.titleCase(product.getName()))).append(" — ");
        if (product.isDiscountActive(now)) {
            sb.append("<s>").append(TelegramFormat.usd(product.getPrice())).append("</s> <b>")
                    .append(TelegramFormat.usd(product.getFinalPrice(now))).append("</b> ")
                    .append(discountBadge(product));
        } else {
            sb.append(TelegramFormat.usd(product.getPrice()));
        }
        sb.append('\n');
    }

    private String discountBadge(Product product) {
        return product.getDiscountType() == DiscountType.PERCENTAGE
                ? "(" + product.getDiscountValue().stripTrailingZeros().toPlainString() + "% OFF)"
                : "(" + TelegramFormat.usd(product.getDiscountValue()) + " OFF)";
    }
}
