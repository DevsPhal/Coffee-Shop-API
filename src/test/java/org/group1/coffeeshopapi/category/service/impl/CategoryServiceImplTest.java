package org.group1.coffeeshopapi.category.service.impl;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.dto.request.CreateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.request.UpdateCategoryRequest;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.mapper.CategoryMapper;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the duplicate-name and in-use-by-products guards — the only real logic in an otherwise
// thin CRUD service.
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryMapper categoryMapper;
    @InjectMocks private CategoryServiceImpl service;

    @Test
    void creatingACategoryWithANameAlreadyInUseIsRejected() {
        when(categoryRepository.existsByNameIgnoreCase("Drinks")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Drinks", null, null), new Admin()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void renamingACategoryToOneAlreadyUsedByAnotherCategoryIsRejected() {
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);
        category.setName("Snacks");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Drinks", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new UpdateCategoryRequest("Drinks", null, null, null), new Admin()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deletingACategoryThatStillHasProductsAssignedIsRejected() {
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("products assigned");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deletingAnEmptyCategorySucceeds() {
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(id)).thenReturn(false);

        service.delete(id);

        verify(categoryRepository).delete(category);
    }
}
