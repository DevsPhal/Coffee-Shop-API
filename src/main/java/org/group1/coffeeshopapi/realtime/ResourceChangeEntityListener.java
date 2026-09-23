package org.group1.coffeeshopapi.realtime;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.feedback.entity.Feedback;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.springframework.beans.factory.ObjectProvider;

import java.util.UUID;

// Hooked onto the watched entities with @EntityListeners, so every save path is covered
// without each service having to remember to publish. Spring Boot builds JPA listeners as
// beans; the publisher is looked up lazily because this listener is created while JPA itself
// is still starting up.
public class ResourceChangeEntityListener {

    private final ObjectProvider<ResourceChangePublisher> publisherProvider;

    public ResourceChangeEntityListener(ObjectProvider<ResourceChangePublisher> publisherProvider) {
        this.publisherProvider = publisherProvider;
    }

    @PostPersist
    public void onCreate(Object entity) {
        record(entity, ChangeType.CREATED);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        record(entity, ChangeType.UPDATED);
    }

    @PostRemove
    public void onDelete(Object entity) {
        record(entity, ChangeType.DELETED);
    }

    private void record(Object entity, ChangeType change) {
        ResourceChangePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        switch (entity) {
            case Product product -> publisher.record(ResourceType.PRODUCT, product.getId(), change);
            case Category category -> publisher.record(ResourceType.CATEGORY, category.getId(), change);
            case Extra extra -> publisher.record(ResourceType.EXTRA, extra.getId(), change);
            // Sizes and attached extras are part of their product as far as clients are concerned.
            case ProductVariant variant -> publisher.record(ResourceType.PRODUCT, productId(variant.getProduct()), ChangeType.UPDATED);
            case ProductExtra productExtra -> publisher.record(ResourceType.PRODUCT, productId(productExtra.getProduct()), ChangeType.UPDATED);
            case Inventory inventory -> publisher.record(ResourceType.INVENTORY, productId(inventory.getProduct()), change);
            case Feedback feedback -> publisher.record(ResourceType.FEEDBACK, feedback.getId(), change);
            default -> { }
        }
    }

    private UUID productId(Product product) {
        return product != null ? product.getId() : null;
    }
}
