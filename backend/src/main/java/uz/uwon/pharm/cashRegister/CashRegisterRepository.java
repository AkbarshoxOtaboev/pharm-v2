package uz.uwon.pharm.cashRegister;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.users.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    List<CashRegister> findByCourierAndCreatedAtBetweenOrderByIdDesc(
            User courier,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query(value = """
    SELECT 
        DATE(c.date_time) AS date,
        SUM(c.order_total_sum) AS total
    FROM cash_registers c
    WHERE (:from IS NULL OR DATE(c.date_time) >= :from)
      AND (:to IS NULL OR DATE(c.date_time) <= :to)
      AND (c.register_status = 'ON_ADMIN')
    GROUP BY DATE(c.date_time)
    ORDER BY DATE(c.date_time)
""", nativeQuery = true)
    List<DailyCashDTO> getDailyCash(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
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
}
