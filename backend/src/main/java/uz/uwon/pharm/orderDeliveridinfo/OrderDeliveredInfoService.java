package uz.uwon.pharm.orderDeliveridinfo;

import java.util.List;

public interface OrderDeliveredInfoService {
    List<OrderDeliveredInfoResponse> getOrderDeliveredInfoResponses(Long orderId);
    void save(Long orderId, String comment);
}
