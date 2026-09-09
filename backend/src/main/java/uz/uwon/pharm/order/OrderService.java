package uz.uwon.pharm.order;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import uz.uwon.pharm.users.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    void save(OrderDTO dto);
    List<OrderResponse> findAll();
    OrderDetailResponse orderDetailResponses(Long orderId);

    OrderPageResponse<OrderResponse> getOrders(Long id, String phone,OrderStatus status, LocalDate fromDate, LocalDate toDate, int page, int size, Long userId);

    void updateStatus(Long id, String status);
    List<OrderDetailResponse> findOrdersByUserAndStatus(Long userId, OrderStatus orderStatus, LocalDateTime startDate, LocalDateTime endDate);
    List<OrderDetailResponse> fetchOrdersByCourierAndStatus(Long userId, OrderStatus orderStatus);
    void setOrderStatusAndComment(Long orderId, String status, String comment, MultipartFile file);
    OrderStatResponse orderStatResponse();

    List<DailyStatDTO> getDailyStats();

    List<BigDecimal> getMonthlySales(int year);

    Long pendingOrderCount(Long courierId);

    void update(Long id, OrderDTO dto);

    BigDecimal orderBonusSum(Long orderId);
}
