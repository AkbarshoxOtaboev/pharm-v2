package uz.uwon.pharm.courierStockHistory;


import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourierStockHistoryRepository extends JpaRepository<CourierStockHistory, Long> {
    List<CourierStockHistory> findCourierStockHistoriesByCourierIdOrderByIdDesc(Long courierId);

    List<CourierStockHistory> findByCourierIdAndCreatedAtBetweenOrderByIdDesc(
            Long courierId,
            LocalDateTime start,
            LocalDateTime end
    );
}
