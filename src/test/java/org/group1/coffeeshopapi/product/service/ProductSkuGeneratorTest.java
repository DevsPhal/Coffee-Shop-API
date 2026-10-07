package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSkuGeneratorTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductSkuGenerator generator;

    @Test
    void firstSkuForACategoryAndNameStartsAtOne() {
        when(productRepository.findSkusStartingWith("FD-COF-IL-")).thenReturn(List.of());

        assertThat(generator.generate(category("Coffee", CategoryGroup.FRESH_DRINK), "Iced Latte"))
                .isEqualTo("FD-COF-IL-001");
    }

    @Test
    void nextSkuContinuesAfterTheHighestExistingSequence() {
        when(productRepository.findSkusStartingWith("SN-PAS-CRO-"))
                .thenReturn(List.of("SN-PAS-CRO-001", "sn-pas-cro-007", "SN-PAS-CRO-X"));

        assertThat(generator.generate(category("Pastry", CategoryGroup.SNACK), "Croissant"))
                .isEqualTo("SN-PAS-CRO-008");
    }

    @Test
    void skusReservedEarlierInTheSameImportAreSkipped() {
        when(productRepository.findSkusStartingWith("BV-SD-MW-")).thenReturn(List.of());

        assertThat(generator.generate(category("Soft Drinks", CategoryGroup.BEVERAGE), "Mineral Water",
                Set.of("BV-SD-MW-001"))).isEqualTo("BV-SD-MW-002");
    }

    @Test
    void namesWithoutLatinLettersFallBackToGenericCodes() {
        when(productRepository.findSkusStartingWith("GN-GEN-PRD-")).thenReturn(List.of());

        assertThat(generator.generate(category("ភេសជ្ជៈ", null), "កាហ្វេ")).isEqualTo("GN-GEN-PRD-001");
    }

    @Test
    void regeneratingASkuThatAlreadyMatchesItsCategoryKeepsIt() {
        Product product = product("Iced Latte", category("Coffee", CategoryGroup.FRESH_DRINK), "fd-cof-il-004");

        assertThat(generator.regenerate(product)).isEqualTo("FD-COF-IL-004");
        verify(productRepository, never()).findSkusStartingWith(anyString());
    }

    @Test
    void regeneratingAfterACategoryChangeIssuesANewSku() {
        Product product = product("Iced Latte", category("Tea", CategoryGroup.FRESH_DRINK), "FD-COF-IL-004");
        when(productRepository.findSkusStartingWith("FD-TEA-IL-")).thenReturn(List.of());

        assertThat(generator.regenerate(product)).isEqualTo("FD-TEA-IL-001");
    }

    @Test
    void variantSkuAppendsTheVariantSuffix() {
        assertThat(ProductSkuGenerator.variantSku("FD-COF-IL-001", VariantLabel.LARGE)).isEqualTo("FD-COF-IL-001-L");
        assertThat(ProductSkuGenerator.variantSku("SN-PAS-CRO-001", VariantLabel.PIECE)).isEqualTo("SN-PAS-CRO-001-PC");
    }

    @Test
    void manualSkuIsNormalizedAndValidated() {
        assertThat(ProductSkuGenerator.normalizeManual(" latte-01 ")).isEqualTo("LATTE-01");
        assertThatThrownBy(() -> ProductSkuGenerator.normalizeManual("bad sku!"))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> ProductSkuGenerator.normalizeManual("  "))
                .isInstanceOf(InvalidOperationException.class);
    }

    private Category category(String name, CategoryGroup group) {
        Category category = new Category();
        category.setName(name);
        category.setCategoryGroup(group);
        return category;
    }

    private Product product(String name, Category category, String sku) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setSku(sku);
        return product;
    }
}
