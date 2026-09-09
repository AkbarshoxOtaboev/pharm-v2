package uz.uwon.pharm.productSaleLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleLogRepository extends JpaRepository<SaleLog, Long> {
    List<SaleLog> findAllByCreatedAtBetweenOrderByIdDesc(LocalDateTime start, LocalDateTime end);
}
