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
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductSizeOptionRepository;
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
    private final ProductSizeOptionRepository sizeOptionRepository;
    private final ProductExtraRepository productExtraRepository;
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
        ProductVariantPolicy.validate(product, request.sizeOptionId(), request.sugarLevel(),
                request.iceLevel(), request.milkType());
        ProductSizeOption sizeOption = resolveEffectiveSizeOption(product, request.sizeOptionId());
        Set<Extra> extras = resolveExtras(product, request.extraIds());

        cart.getItems().stream()
                .filter(item -> sameLineItem(item, product.getId(), sizeOption.getId(),
                        request.sugarLevel(), request.iceLevel(), request.milkType(), extras))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),
                        () -> {
                            CartItem item = new CartItem();
                            item.setProduct(product);
                            item.setQuantity(request.quantity());
                            item.setSizeOption(sizeOption);
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
        if (request.sizeOptionId() != null) {
            item.setSizeOption(resolveSizeOption(item.getProduct().getId(), request.sizeOptionId()));
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
        // Validate against what the caller actually asked to change here, not the item's current
        // (possibly auto-resolved, see checkout()'s comment) stored state — same reasoning as
        // addItem validating request.sizeOptionId() rather than the resolved ProductSizeOption.
        ProductVariantPolicy.validate(item.getProduct(), request.sizeOptionId(), request.sugarLevel(),
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

        // sizeOption is always set once an item's in the cart — even for a SNACK/no-group product,
        // which still needs one internally for pricing (see resolveEffectiveSizeOption) despite
        // never letting the customer choose it. Forwarding it here unconditionally would make
        // buildOrder's re-validation see it as an explicit (and, for that product, forbidden)
        // choice — only forward it where the product's group actually allows picking a size.
        List<OrderItemRequest> items = cart.getItems().stream()
                .map(item -> new OrderItemRequest(item.getProduct().getId(), item.getQuantity(),
                        ProductVariantPolicy.allowsSizeChoice(item.getProduct()) && item.getSizeOption() != null
                                ? item.getSizeOption().getId() : null,
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

    private ProductSizeOption resolveSizeOption(UUID productId, UUID sizeOptionId) {
        if (sizeOptionId == null) {
            return null;
        }
        ProductSizeOption sizeOption = sizeOptionRepository.findByIdAndProductId(sizeOptionId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Size option not found: " + sizeOptionId));
        if (sizeOption.getStatus() != Status.ACTIVE) {
            throw new InvalidOperationException("Size option '" + sizeOption.getName() + "' is not available");
        }
        return sizeOption;
    }

    // Explicit choice, or — where the product only has one active option — that option
    // automatically. See ProductPriceResolver.
    private ProductSizeOption resolveEffectiveSizeOption(Product product, UUID sizeOptionId) {
        if (sizeOptionId != null) {
            return resolveSizeOption(product.getId(), sizeOptionId);
        }
        List<ProductSizeOption> activeOptions = sizeOptionRepository
                .findByProductIdAndStatusOrderBySortOrderAscNameAsc(product.getId(), Status.ACTIVE);
        return ProductPriceResolver.resolveEffective(product, null, activeOptions);
    }

    private boolean sameLineItem(CartItem item, UUID productId, UUID sizeOptionId,
            SugarLevel sugarLevel, IceLevel iceLevel, MilkType milkType, Set<Extra> extras) {
        UUID existingSizeOptionId = item.getSizeOption() != null ? item.getSizeOption().getId() : null;
        return item.getProduct().getId().equals(productId)
                && Objects.equals(existingSizeOptionId, sizeOptionId)
                && Objects.equals(item.getSugarLevel(), sugarLevel)
                && Objects.equals(item.getIceLevel(), iceLevel)
                && Objects.equals(item.getMilkType(), milkType)
                && extraIds(item.getSelectedExtras()).equals(extraIds(extras));
    }

    private Set<UUID> extraIds(Set<Extra> extras) {
        return extras.stream().map(Extra::getId).collect(Collectors.toSet());
    }

    // Delegates the actual matching/validation to ProductExtraResolver (shared with
    // OrderServiceImpl) — this just supplies the product's currently active, attached extras.
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
            ProductSizeOption sizeOption = item.getSizeOption();
            Set<Extra> selectedExtras = item.getSelectedExtras();
            List<CartItemExtraResponse> extras = selectedExtras.stream()
                    .map(extra -> new CartItemExtraResponse(extra.getId(), extra.getName(), extra.getPrice()))
                    .toList();
            BigDecimal extrasTotal = selectedExtras.stream().map(Extra::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = product.getFinalPrice(sizeOption.getPrice(), now).add(extrasTotal);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
            items.add(new CartItemResponse(
                    item.getId(), product.getId(), product.getName(), product.getImageUrl(),
                    unitPrice, item.getQuantity(), subtotal,
                    sizeOption.getId(), sizeOption.getName(),
                    item.getSugarLevel(), item.getIceLevel(), item.getMilkType(), extras));
        }

        return new CartResponse(cart.getId(), items, total);
    }
}
