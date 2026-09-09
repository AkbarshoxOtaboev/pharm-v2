package uz.uwon.pharm.orderitem;

import lombok.*;
import uz.uwon.pharm.product.UnitType;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalSum;
    private Boolean isBonus;
    private String type;
    private BigDecimal storeQuantity;
}
