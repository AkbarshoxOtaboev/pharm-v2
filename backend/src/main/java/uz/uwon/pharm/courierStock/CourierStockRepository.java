package uz.uwon.pharm.courierStock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourierStockRepository extends JpaRepository<CourierStock, Long> {
    List<CourierStock> findCourierStockByCourierIdOrderByIdAsc(Long courierId);
    Optional<CourierStock> findByCourierIdAndProductId(Long courierId, Long productId);
    List<CourierStock> findByCourierIdAndProductNameContainingIgnoreCaseAndQuantityGreaterThan(
            Long courierId, String name, BigDecimal quantity
    );
    List<CourierStock> findAllByCourierIdAndProductIdIn(Long courierId, List<Long> productIds);
}
