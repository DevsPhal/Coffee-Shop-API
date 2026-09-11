package org.group1.coffeeshopapi.extra.service;

import org.group1.coffeeshopapi.extra.dto.request.AttachProductExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateProductExtraRequest;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;

import java.util.List;
import java.util.UUID;

public interface ProductExtraService {
    ProductExtraResponse attach(UUID productId, AttachProductExtraRequest request);
    List<ProductExtraResponse> list(UUID productId);
    ProductExtraResponse update(UUID productId, UUID id, UpdateProductExtraRequest request);
    void detach(UUID productId, UUID id);
}
