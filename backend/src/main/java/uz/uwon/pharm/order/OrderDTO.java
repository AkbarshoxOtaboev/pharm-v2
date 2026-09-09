package uz.uwon.pharm.order;

import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.orderitem.OrderItemDTO;

import java.util.List;

@Getter
@Setter
public class OrderDTO {
    private Long customerId;
    private String fullName;
    private String phone;
    private String address;
    private String home;
    private String entrance;
    private String apartment;
    private String floor;
    private String orientations;
    private PaymentType paymentType;
    private Long courierId;
    private List<OrderItemDTO> items;
}
