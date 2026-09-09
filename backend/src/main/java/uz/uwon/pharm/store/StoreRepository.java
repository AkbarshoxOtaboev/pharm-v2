package uz.uwon.pharm.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByIdAndStatus(Long id, Status status);
    List<Store> findAllByStatusOrderByIdAsc(Status status);
    List<Store> findByProduct_NameContainingIgnoreCaseAndQuantityGreaterThan(
            String name, BigDecimal quantity
    );

    List<Store> findAllByStatusAndQuantityGreaterThan(Status status, BigDecimal quantityIsGreaterThan);
    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM store) as totalProducts,
                sum(quantity) as totalQuantity,
                SUM(total_amount) as totalCostSum,
                SUM(
                    CASE
                        WHEN DATE(created_at) = CURRENT_DATE
                        THEN quantity
                        ELSE 0
                    END
                ) as todayAddedQuantity
            FROM store
            """, nativeQuery = true)
    StoreStatisticsResponse getStoreStatistics();
}
