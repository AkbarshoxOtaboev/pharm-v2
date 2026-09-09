package uz.uwon.pharm.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.orderitem.OrderItemResponse;
import uz.uwon.pharm.users.UserResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class OrderDetailResponse {
    private Long orderId;
    private String fullName;
    private String phone;
    private String orderNumber;
    private LocalDateTime createdDate;
    private String address;
    private String house;
    private String entrance;
    private String apartment;
    private String floor;
    private String orientation;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private UserResponse courier;
    private List<OrderItemResponse> items;
}
