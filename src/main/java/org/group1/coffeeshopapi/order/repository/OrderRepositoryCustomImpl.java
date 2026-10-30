package org.group1.coffeeshopapi.order.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.order.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OrderStatusTotal> totalsByStatus(Specification<Order> specification) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<Order> root = query.from(Order.class);
        Path<OrderStatus> status = root.get("status");
        Expression<Long> count = builder.count(root);
        Expression<BigDecimal> total = builder.sum(root.get("totalAmount"));

        query.multiselect(status, count, total).groupBy(status);
        Predicate predicate = specification.toPredicate(root, query, builder);
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new OrderStatusTotal(row.get(status), row.get(count), row.get(total)))
                .toList();
    }
}
