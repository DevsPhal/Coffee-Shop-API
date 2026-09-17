package org.group1.coffeeshopapi.cart.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.cart.dto.request.AddCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.request.CheckoutRequest;
import org.group1.coffeeshopapi.cart.dto.request.UpdateCartItemRequest;
import org.group1.coffeeshopapi.cart.dto.response.CartItemExtraResponse;
import org.group1.coffeeshopapi.cart.dto.response.CartItemResponse;
import org.group1.coffeeshopapi.cart.dto.response.CartResponse;
import org.group1.coffeeshopapi.cart.entity.Cart;
import org.group1.coffeeshopapi.cart.entity.CartItem;
import org.group1.coffeeshopapi.cart.repository.CartRepository;
import org.group1.coffeeshopapi.cart.service.CartService;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.product.service.ProductExtraResolver;
import org.group1.coffeeshopapi.product.service.ProductPriceResolver;
import org.group1.coffeeshopapi.product.service.ProductVariantPolicy;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductExtraRepository productExtraRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;

    @Override
    public CartResponse getCart(UUID customerId) {
        return toResponse(getOrCreateCart(customerId));
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID customerId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        if (product.getStatus() != Status.ACTIVE) {
            throw new InvalidOperationException("Product '" + product.getName() + "' is not available");
        }
        // A customer with a stale product page open shouldn't be able to add something sold out.
        BigDecimal stockOnHand = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getQuantityOnHand)
                .orElse(BigDecimal.ZERO);
        if (stockOnHand.signum() <= 0) {
            throw new InvalidOperationException("Product '" + product.getName() + "' is out of stock");
        }
        ProductVariantPolicy.validate(product, request.variantId(), request.sugarLevel(),
                request.iceLevel(), request.milkType());
        ProductVariant variant = resolveEffectiveVariant(product, request.variantId());
        Set<Extra> extras = resolveExtras(product, request.extraIds());

        cart.getItems().stream()
                .filter(item -> sameLineItem(item, product.getId(), variant.getId(),
                        request.sugarLevel(), request.iceLevel(), request.milkType(), extras))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),
                        () -> {
                            CartItem item = new CartItem();
                            item.setProduct(product);
                            item.setQuantity(request.quantity());
                            item.setVariant(variant);
                            item.setSugarLevel(request.sugarLevel());
                            item.setIceLevel(request.iceLevel());
                            item.setMilkType(request.milkType());
                            item.setExtraSelection(extras);
                            cart.addItem(item);
                        });

        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = findItem(cart, itemId);
        item.setQuantity(request.quantity());
        if (request.variantId() != null) {
            item.setVariant(resolveVariant(item.getProduct().getId(), request.variantId()));
        }
        if (request.sugarLevel() != null) {
            item.setSugarLevel(request.sugarLevel());
        }
        if (request.iceLevel() != null) {
            item.setIceLevel(request.iceLevel());
        }
        if (request.milkType() != null) {
            item.setMilkType(request.milkType());
        }
        if (request.extraIds() != null) {
            item.setExtraSelection(resolveExtras(item.getProduct(), request.extraIds()));
        }
        // Validate against what the caller actually asked to change, not the item's stored state.
        ProductVariantPolicy.validate(item.getProduct(), request.variantId(), request.sugarLevel(),
                request.iceLevel(), request.milkType());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(UUID customerId, UUID itemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse clearCart(UUID customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public OrderResponse checkout(UUID customerId, CheckoutRequest request) {
        Cart cart = getOrCreateCart(customerId);
        if (cart.getItems().isEmpty()) {
            throw new InvalidOperationException("Cart is empty");
        }

        // Only forward the variant id where the product actually allows picking a size — a
        // SNACK-type product has one auto-resolved internally, but never let the customer choose.
        List<OrderItemRequest> items = cart.getItems().stream()
                .map(item -> new OrderItemRequest(item.getProduct().getId(), item.getQuantity(),
                        ProductVariantPolicy.allowsSizeChoice(item.getProduct()) && item.getVariant() != null
                                ? item.getVariant().getId() : null,
                        null,
                        item.getSugarLevel(), item.getIceLevel(), item.getMilkType(),
                        item.getSelectedExtras().stream().map(Extra::getId).toList()))
                .toList();

        OrderResponse order = orderService.createForCustomer(
                new CreateOrderRequest(items, request.note(), request.delivery()), customerId,
                request.deliveryLatitude(), request.deliveryLongitude());

        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }

    private ProductVariant resolveVariant(UUID productId, UUID variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
        if (variant.getStatus() != Status.ACTIVE) {
            throw new InvalidOperationException("Variant '" + variant.getName() + "' is not available");
        }
        return variant;
    }

    // Explicit choice, or the product's one active option if it only has one.
    private ProductVariant resolveEffectiveVariant(Product product, UUID variantId) {
        if (variantId != null) {
            return resolveVariant(product.getId(), variantId);
        }
        List<ProductVariant> activeOptions = variantRepository
                .findByProductIdAndStatusOrderBySortOrderAscNameAsc(product.getId(), Status.ACTIVE);
        return ProductPriceResolver.resolveEffective(product, null, activeOptions);
    }

    private boolean sameLineItem(CartItem item, UUID productId, UUID variantId,
            SugarLevel sugarLevel, IceLevel iceLevel, MilkType milkType, Set<Extra> extras) {
        UUID existingVariantId = item.getVariant() != null ? item.getVariant().getId() : null;
        return item.getProduct().getId().equals(productId)
                && Objects.equals(existingVariantId, variantId)
                && Objects.equals(item.getSugarLevel(), sugarLevel)
                && Objects.equals(item.getIceLevel(), iceLevel)
                && Objects.equals(item.getMilkType(), milkType)
                && extraIds(item.getSelectedExtras()).equals(extraIds(extras));
    }

    private Set<UUID> extraIds(Set<Extra> extras) {
        return extras.stream().map(Extra::getId).collect(Collectors.toSet());
    }

    private Set<Extra> resolveExtras(Product product, List<UUID> extraIds) {
        if (extraIds == null || extraIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<ProductExtra> attached = productExtraRepository
                .findByProductIdAndExtraIdInAndStatus(product.getId(), extraIds, Status.ACTIVE);
        return new LinkedHashSet<>(ProductExtraResolver.resolve(product, extraIds, attached));
    }

    private Cart getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomer_Id(customerId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCustomer(customerRepository.getReferenceById(customerId));
            return cartRepository.save(cart);
        });
    }

    private CartItem findItem(Cart cart, UUID itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }

    private CartResponse toResponse(Cart cart) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemResponse> items = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            Set<Extra> selectedExtras = item.getSelectedExtras();
            List<CartItemExtraResponse> extras = selectedExtras.stream()
                    .map(extra -> new CartItemExtraResponse(extra.getId(), extra.getName(), extra.getPrice()))
                    .toList();
            BigDecimal extrasTotal = selectedExtras.stream().map(Extra::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = product.getFinalPrice(variant.getPrice(), now).add(extrasTotal);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
            items.add(new CartItemResponse(
                    item.getId(), product.getId(), product.getName(), product.getImageUrl(),
                    unitPrice, item.getQuantity(), subtotal,
                    variant.getId(), variant.getName(),
                    item.getSugarLevel(), item.getIceLevel(), item.getMilkType(), extras));
        }

        return new CartResponse(cart.getId(), items, total);
    }
}
