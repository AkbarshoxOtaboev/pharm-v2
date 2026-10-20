package uz.uwon.pharm.orderitem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.order.OrderStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    interface ReservedStockView {
        Long getProductId();
        BigDecimal getQuantity();
        Long getOrdersCount();
    }

    @Query("""
            SELECT oi.product.id AS productId,
                   COALESCE(SUM(oi.quantity), 0) AS quantity,
                   COUNT(DISTINCT oi.order.id) AS ordersCount
            FROM OrderItem oi
            WHERE oi.order.user.id = :courierId
              AND oi.order.orderStatus IN :statuses
            GROUP BY oi.product.id
            """)
    List<ReservedStockView> reservedByCourier(@Param("courierId") Long courierId,
                                              @Param("statuses") Collection<OrderStatus> statuses);
}
