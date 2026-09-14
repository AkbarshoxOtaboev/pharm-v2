package uz.uwon.pharm.courierStock;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourierStockItem {
    private Long productId;
    private BigDecimal quantity;
}
