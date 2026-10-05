package uz.uwon.pharm.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

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
}
