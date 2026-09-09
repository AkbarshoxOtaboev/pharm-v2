package uz.uwon.pharm.courierStock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CourierStockDTO {
    private Long courierId;
    private List<CourierStockItem> items;
}
