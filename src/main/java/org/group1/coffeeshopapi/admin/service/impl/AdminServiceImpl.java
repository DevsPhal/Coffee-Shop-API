package org.group1.coffeeshopapi.admin.service.impl;

import org.group1.coffeeshopapi.admin.dto.response.AdminDashboardResponse;
import org.group1.coffeeshopapi.admin.service.AdminService;
import org.group1.coffeeshopapi.categories.repository.CategoryRepository;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	@Override
	public AdminDashboardResponse getDashboardSummary() {
		long totalProducts = productRepository.count();
		long activeProducts = productRepository.findAllActive().size();
		long totalCategories = categoryRepository.count();

		return new AdminDashboardResponse(totalProducts, activeProducts, totalCategories, 0L);
	}
}
