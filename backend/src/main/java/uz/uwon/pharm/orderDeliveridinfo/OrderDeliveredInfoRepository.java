package uz.uwon.pharm.orderDeliveridinfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.order.Order;

import java.util.List;

@Repository
public interface OrderDeliveredInfoRepository extends JpaRepository<OrderDeliveredInfo, Long> {
    List<OrderDeliveredInfo> findAllByOrderOrderByIdDesc(Order order);
}
