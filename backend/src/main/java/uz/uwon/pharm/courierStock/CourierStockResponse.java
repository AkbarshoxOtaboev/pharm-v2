package uz.uwon.pharm.courierStock;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.product.UnitType;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class CourierStockResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal totalAmount;
    private UnitType unitType;
}
