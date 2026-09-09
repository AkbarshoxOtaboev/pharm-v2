package uz.uwon.pharm.orderitem;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemDTO {
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal productCost;
    private Boolean isBonus = false;

}
