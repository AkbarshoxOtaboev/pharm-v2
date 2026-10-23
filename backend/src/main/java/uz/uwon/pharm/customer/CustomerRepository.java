package uz.uwon.pharm.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdAndStatus(Long id, Status status);
    List<Customer> findAllByStatus(Status status);
    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByPhoneAndStatus(String phone, Status status);

    @Query(value = """
                SELECT c.id AS id, c.fullName AS fullName, c.phone AS phone, c.birthDate AS birthDate,
                       c.description AS description, c.gender AS gender, c.type AS type, c.status AS status,
                       c.createdAt AS createdAt,
                       COUNT(o.id) AS orderCount, COALESCE(SUM(o.totalSum), 0) AS orderSum
                FROM Customer c
                LEFT JOIN Order o ON o.customer = c
                    AND o.orderStatus = uz.uwon.pharm.order.OrderStatus.DELIVERED
                WHERE :all = true OR LOWER(c.fullName) LIKE :name OR c.phone LIKE :phone
                GROUP BY c.id, c.fullName, c.phone, c.birthDate, c.description, c.gender, c.type, c.status, c.createdAt
                ORDER BY c.id DESC
            """,
            countQuery = """
                SELECT COUNT(c) FROM Customer c
                WHERE :all = true OR LOWER(c.fullName) LIKE :name OR c.phone LIKE :phone
            """)
    Page<CustomerStatsView> searchWithStats(@Param("all") boolean all,
                                            @Param("name") String name,
                                            @Param("phone") String phone,
                                            Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = 0")
    long countActiveCustomers();

    @Query(value = """
            SELECT
                COUNT(*) AS totalCustomers,
                COUNT(*) FILTER (WHERE s.order_count > 0) AS activeBase,
                COUNT(*) FILTER (WHERE s.order_count = 0) AS noOrders
            FROM (
                SELECT c.id, COUNT(o.id) AS order_count
                FROM customers c
                LEFT JOIN orders o
                    ON o.customer_id = c.id
                    AND o.created_at >= :from
                    AND o.created_at < :to
                    AND o.order_status <> 'DELETED'
                WHERE c.status IS NULL OR c.status <> 2
                GROUP BY c.id
            ) s
            """, nativeQuery = true)
    CustomerSalesSummaryView salesSummary(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                c.id AS id,
                c.full_name AS fullName,
                c.phone AS phone,
                COUNT(o.id) AS orderCount,
                COUNT(o.id) FILTER (WHERE o.order_status = 'CANCELLED') AS cancelledCount,
                COUNT(o.id) FILTER (WHERE o.order_status = 'DELIVERED') AS deliveredCount,
                COALESCE(SUM(o.total_sum) FILTER (WHERE o.order_status = 'DELIVERED'), 0) AS orderSum
            FROM customers c
            LEFT JOIN orders o
                ON o.customer_id = c.id
                AND o.created_at >= :from
                AND o.created_at < :to
                AND o.order_status <> 'DELETED'
            WHERE c.status IS NULL OR c.status <> 2
            GROUP BY c.id, c.full_name, c.phone
            HAVING (
                (:segment = 'ORDERED' AND COUNT(o.id) > 0)
                OR (:segment = 'NONE' AND COUNT(o.id) = 0)
                OR (:segment = 'CANCELLED' AND COUNT(o.id) FILTER (WHERE o.order_status = 'CANCELLED') > 0)
            )
            ORDER BY COUNT(o.id) DESC, c.full_name ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT c.id
                FROM customers c
                LEFT JOIN orders o
                    ON o.customer_id = c.id
                    AND o.created_at >= :from
                    AND o.created_at < :to
                    AND o.order_status <> 'DELETED'
                WHERE c.status IS NULL OR c.status <> 2
                GROUP BY c.id
                HAVING (
                    (:segment = 'ORDERED' AND COUNT(o.id) > 0)
                    OR (:segment = 'NONE' AND COUNT(o.id) = 0)
                    OR (:segment = 'CANCELLED' AND COUNT(o.id) FILTER (WHERE o.order_status = 'CANCELLED') > 0)
                )
            ) sales_count
            """,
            nativeQuery = true)
    Page<CustomerSalesRowView> salesPage(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("segment") String segment,
            Pageable pageable);
}
