package org.group1.coffeeshopapi.order.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.group1.coffeeshopapi.common.enums.OrderChannel;
import org.group1.coffeeshopapi.order.dto.request.OrderSearchFilter;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OrderSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private OrderSpecifications() {
    }

    public static Specification<Order> matching(OrderSearchFilter filter) {
        return (root, query, builder) -> {
            String search = filter.search() != null ? filter.search().trim() : "";
            Join<Order, Customer> customer = joinCustomer(root, query, !search.isEmpty());

            List<Predicate> predicates = new ArrayList<>();
            if (filter.baristaId() != null) {
                predicates.add(builder.equal(root.get("handledBy"), filter.baristaId()));
            }
            if (filter.customerId() != null) {
                predicates.add(builder.equal(root.get("customer").get("id"), filter.customerId()));
            }
            if (filter.statuses() != null && !filter.statuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.statuses()));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), filter.to().plusDays(1).atStartOfDay()));
            }
            if (filter.channel() != null) {
                predicates.add(filter.channel() == OrderChannel.ONLINE
                        ? builder.isNotNull(root.get("customer"))
                        : builder.isNull(root.get("customer")));
            }
            if (filter.fulfillment() != null) {
                predicates.add(builder.equal(root.get("fulfillmentMethod"), filter.fulfillment()));
            }
            if (filter.paymentMethod() != null) {
                predicates.add(builder.equal(root.get("paymentMethod"), filter.paymentMethod()));
            }
            if (!search.isEmpty()) {
                predicates.add(matchesSearch(root, customer, builder, search));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    // A page of orders loads each customer in the same query, like the repository's other list queries; count
    // and per-status queries can't fetch, so they join the customer only when a search needs the name.
    @SuppressWarnings("unchecked")
    private static Join<Order, Customer> joinCustomer(Root<Order> root, CriteriaQuery<?> query, boolean searching) {
        if (Order.class.equals(query.getResultType())) {
            return (Join<Order, Customer>) root.<Order, Customer>fetch("customer", JoinType.LEFT);
        }
        return searching ? root.join("customer", JoinType.LEFT) : null;
    }

    // Receipts and the queue show an order as "#A1B2C3D4", the start of its id, so a number matches that prefix;
    // anything else matches part of the customer's or the contact's name.
    private static Predicate matchesSearch(Root<Order> root, Join<Order, Customer> customer,
            CriteriaBuilder builder, String search) {
        String term = search.toLowerCase(Locale.ROOT);
        String orderNumber = term.replaceFirst("^(#|ord-)", "");
        String anywhere = "%" + escapeLike(term) + "%";
        // as() only relabels the type; cast() makes the database turn the UUID into text.
        Expression<String> id = builder.lower(((JpaExpression<?>) root.get("id")).cast(String.class));
        return builder.or(
                builder.like(id, escapeLike(orderNumber) + "%", LIKE_ESCAPE),
                builder.like(builder.lower(customer.get("fullName")), anywhere, LIKE_ESCAPE),
                builder.like(builder.lower(root.get("contactName")), anywhere, LIKE_ESCAPE));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
