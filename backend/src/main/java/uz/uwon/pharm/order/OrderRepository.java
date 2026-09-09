package uz.uwon.pharm.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.users.CourierMonthlyStatProjection;
import uz.uwon.pharm.users.CourierStatsDto;
import uz.uwon.pharm.users.User;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {


    List<Order> findOrdersByUserAndOrderStatusOrderByIdDesc(User user, OrderStatus orderStatus);

    Optional<Order> findOrderById(Long id);

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                WHERE o.orderStatus = uz.uwon.pharm.order.OrderStatus.SHIPPED
                AND o.user.id = :courierId
            """)
    Long pendingOrderCount(@Param("courierId") Long courierId);

    @Query("""
                SELECT 
                    COUNT(o) as totalOrders,
            
                    SUM(CASE WHEN o.orderStatus = uz.uwon.pharm.order.OrderStatus.DELIVERED THEN 1 ELSE 0 END) as deliveredCount,
                    SUM(CASE WHEN o.orderStatus = uz.uwon.pharm.order.OrderStatus.CANCELLED THEN 1 ELSE 0 END) as canceledCount,
                    SUM(CASE WHEN o.orderStatus = uz.uwon.pharm.order.OrderStatus.PENDING THEN 1 ELSE 0 END) as pendingCount,
            
                    SUM(CASE 
                        WHEN o.orderStatus = uz.uwon.pharm.order.OrderStatus.DELIVERED AND o.paymentType = 0 
                        THEN o.totalSum ELSE 0 END) as cashTotal,
            
                    SUM(CASE 
                        WHEN o.orderStatus = uz.uwon.pharm.order.OrderStatus.DELIVERED 
                        THEN o.totalSum ELSE 0 END) as totalSum
            
                FROM Order o
                WHERE o.user.id = :userId
                  AND o.createdAt >= :start AND o.createdAt < :end
            """)
    CourierStatsDto getCourierStats(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    @Query("""
                SELECT o
                FROM Order o
                WHERE o.user.id = :userId
                  AND o.orderStatus = :status
                  AND o.createdAt BETWEEN :start AND :end
                ORDER BY o.id DESC
            """)
    List<Order> findOrdersByUserIdAndStatusAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
            SELECT 
                (SELECT COUNT(*) FROM customers) as clientsCount,
            
                COUNT(*) as ordersCount,
            
                SUM(CASE WHEN order_status = 'DELIVERED'  THEN 1 ELSE 0 END) as deliveredOrdersCount,
            
                SUM(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledOrdersCount,
            
                SUM(CASE WHEN order_status = 'SHIPPED' THEN 1 ELSE 0 END) as dailyDeliveredOrdersTotalSum,
            
                SUM(CASE WHEN order_status = 'DELIVERED' 
                    AND created_at BETWEEN :startMonth AND :endMonth 
                    THEN total_sum ELSE 0 END) as monthlyDeliveredOrdersTotalSum
            
            FROM orders
            WHERE created_at BETWEEN :startMonth AND :endMonth
            """, nativeQuery = true)
    OrderStatResponse getStatistics(
            LocalDate startDay,
            LocalDate endDay,
            LocalDate startMonth,
            LocalDate endMonth
    );


    @Query(value = """
                SELECT 
                    DATE(o.updated_at) as date,
            
                    COUNT(DISTINCT o.id) as ordersCount,
            
                    COUNT(DISTINCT CASE 
                        WHEN o.order_status = 'DELIVERED' THEN o.id END) as deliveredCount,
            
                    COUNT(DISTINCT CASE 
                        WHEN o.order_status = 'CANCELLED' THEN o.id END) as cancelledCount,
            
                    COALESCE(SUM(CASE 
                        WHEN o.order_status = 'DELIVERED' AND oi.is_bonus = false
                        THEN oi.total_sum ELSE 0 END), 0) as deliveredSum,
            
                    COALESCE(SUM(CASE 
                        WHEN o.order_status = 'CANCELLED' AND oi.is_bonus = false
                        THEN oi.total_sum ELSE 0 END), 0) as cancelledSum,
                    COALESCE(SUM(CASE
                        WHEN o.order_status = 'DELIVERED' AND oi.is_bonus = true
                        THEN (COALESCE(oi.price,0) * COALESCE(oi.quantity,0)) ELSE 0 END), 0) as deliveredBonusSum
            
                FROM orders o
                LEFT JOIN order_items oi ON oi.order_id = o.id
            
                WHERE DATE(o.updated_at) BETWEEN :startDate AND :endDate
            
                GROUP BY DATE(o.updated_at)
                ORDER BY DATE(o.updated_at)
            """, nativeQuery = true)
    List<DailyStatDTO> getDailyStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            SELECT
                CAST(EXTRACT(MONTH FROM o.created_at) AS integer) AS month,
                COALESCE(SUM(CASE
                    WHEN o.order_status = 'DELIVERED' THEN o.total_sum
                    ELSE 0 END), 0) AS totalSum
            FROM orders o
            WHERE EXTRACT(YEAR FROM o.created_at) = :year
            GROUP BY EXTRACT(MONTH FROM o.created_at)
            ORDER BY month
            """, nativeQuery = true)
    List<MonthlyStatDTO> getMonthlySales(@Param("year") int year);



    @Query(value = """
                SELECT 
                    u.id AS courierId,
                    u.full_name AS courierName,
            
                    COUNT(o.id) AS allOrders,
            
                    SUM(CASE 
                        WHEN o.order_status = 'DELIVERED' THEN 1 
                        ELSE 0 
                    END) AS deliveredOrders,
            
                    SUM(CASE 
                        WHEN o.order_status = 'CANCELLED' THEN 1 
                        ELSE 0 
                    END) AS cancelledOrders,
            
                    COALESCE(SUM(
                        CASE 
                            WHEN o.order_status = 'DELIVERED' THEN o.total_sum 
                            ELSE 0 
                        END
                    ), 0) AS deliveredTotalSum
            
                FROM _users u
                LEFT JOIN orders o 
                    ON u.id = o.user_id
                    AND o.created_at BETWEEN :startDate AND :endDate
            
                WHERE u.role = 'COURIER'
            
                GROUP BY u.id, u.full_name
                ORDER BY u.full_name
            """, nativeQuery = true)
    List<CourierMonthlyStatProjection> getCourierStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
