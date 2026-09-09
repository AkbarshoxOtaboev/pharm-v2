package uz.uwon.pharm.courierStock;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReturnDTO {
    private Long courierId;
    private Long productId;
    private BigDecimal quantity;
}
