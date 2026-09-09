package uz.uwon.pharm.order;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uz.uwon.pharm.customer.Customer;
import uz.uwon.pharm.users.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class OrderSpecification {

    public static Specification<Order> filter(
            Long id,
            String phone,
            OrderStatus status,
            LocalDate from,
            LocalDate to,
            Long userId
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (id != null) {
                predicates.add(cb.equal(root.get("id"), id));
            }

            if (phone != null && !phone.isBlank()) {
                Join<Order, Customer> customer = root.join("customer");
                predicates.add(cb.like(
                        cb.lower(customer.get("phone")),
                        "%" + phone.toLowerCase().trim() + "%"
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        from.atStartOfDay()
                ));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        to.atTime(23, 59, 59)
                ));
            }
            // 🔥 USER FILTER
            if (userId != null) {
                Join<Order, User> userJoin = root.join("user", JoinType.LEFT);
                predicates.add(cb.equal(userJoin.get("id"), userId));
            }


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
