package uz.uwon.pharm.courierStockHistory;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourierStockHistoryRepository extends JpaRepository<CourierStockHistory, Long>,
        JpaSpecificationExecutor<CourierStockHistory> {
    List<CourierStockHistory> findCourierStockHistoriesByCourierIdOrderByIdDesc(Long courierId);

    List<CourierStockHistory> findByCourierIdAndCreatedAtBetweenOrderByIdDesc(
            Long courierId,
            LocalDateTime start,
            LocalDateTime end
    );
}
