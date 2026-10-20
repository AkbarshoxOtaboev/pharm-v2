package uz.uwon.pharm.cashRegister;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.users.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long>,
        JpaSpecificationExecutor<CashRegister> {
    List<CashRegister> findByCourierAndCreatedAtBetweenOrderByIdDesc(
            User courier,
            LocalDateTime from,
            LocalDateTime to
    );

    List<CashRegister> findByCourierIdAndRegisterStatus(Long courierId, CashRegisterStatus registerStatus);

    @Query(value = """
    SELECT 
        DATE(c.date_time) AS date,
        SUM(c.order_total_sum) AS total,
        COUNT(*) AS ordersCount
    FROM cash_registers c
    WHERE (CAST(:from AS date) IS NULL OR DATE(c.date_time) >= CAST(:from AS date))
      AND (CAST(:to AS date) IS NULL OR DATE(c.date_time) <= CAST(:to AS date))
      AND c.register_status = 'ON_ADMIN'
      AND c.status = 'ACTIVE'
    GROUP BY DATE(c.date_time)
    ORDER BY DATE(c.date_time)
""", nativeQuery = true)
    List<DailyCashDTO> getDailyCash(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query(value = """
    SELECT
        DATE(c.date_time) AS date,
        SUM(c.order_total_sum) AS total,
        COUNT(*) AS ordersCount
    FROM cash_registers c
    WHERE c.date_time >= :from AND c.date_time < :to
      AND c.register_status = 'ON_ADMIN'
      AND c.status = 'ACTIVE'
    GROUP BY DATE(c.date_time)
    ORDER BY DATE(c.date_time) DESC
""", countQuery = """
    SELECT COUNT(DISTINCT DATE(c.date_time))
    FROM cash_registers c
    WHERE c.date_time >= :from AND c.date_time < :to
      AND c.register_status = 'ON_ADMIN'
      AND c.status = 'ACTIVE'
""", nativeQuery = true)
    Page<DailyCashDTO> dailyCashPage(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
    SELECT COALESCE(SUM(o.orderTotalSum), 0)
    FROM CashRegister o
    WHERE o.courier = :courier
      AND o.registerStatus = 'ON_COURIER'
      AND o.createdAt BETWEEN :from AND :to
""")
    BigDecimal totalCourierCash(@Param("courier") User courier,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    /** Per-courier balance: money still on the courier (all time) and money handed over within [from, to). */
    @Query("""
    SELECT c.courier.id AS courierId,
           c.courier.fullName AS courierName,
           COALESCE(SUM(CASE WHEN c.registerStatus = uz.uwon.pharm.cashRegister.CashRegisterStatus.ON_COURIER
                             THEN c.orderTotalSum ELSE 0 END), 0) AS onCourierSum,
           SUM(CASE WHEN c.registerStatus = uz.uwon.pharm.cashRegister.CashRegisterStatus.ON_COURIER
                    THEN 1 ELSE 0 END) AS onCourierCount,
           MIN(CASE WHEN c.registerStatus = uz.uwon.pharm.cashRegister.CashRegisterStatus.ON_COURIER
                    THEN c.createdAt END) AS oldestOnCourierAt,
           COALESCE(SUM(CASE WHEN c.registerStatus = uz.uwon.pharm.cashRegister.CashRegisterStatus.ON_ADMIN
                                  AND c.dateTime >= :from AND c.dateTime < :to
                             THEN c.orderTotalSum ELSE 0 END), 0) AS handedSum,
           SUM(CASE WHEN c.registerStatus = uz.uwon.pharm.cashRegister.CashRegisterStatus.ON_ADMIN
                         AND c.dateTime >= :from AND c.dateTime < :to
                    THEN 1 ELSE 0 END) AS handedCount,
           MAX(CASE WHEN c.registerStatus = uz.uwon.pharm.cashRegister.CashRegisterStatus.ON_ADMIN
                    THEN c.dateTime END) AS lastHandedAt
    FROM CashRegister c
    WHERE c.status = uz.uwon.pharm.utils.Status.ACTIVE
    GROUP BY c.courier.id, c.courier.fullName
""")
    List<CourierCashView> courierBalances(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
