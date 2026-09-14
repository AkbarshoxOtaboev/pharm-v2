package uz.uwon.pharm.courierStock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierStockDTO {
    private Long courierId;
    private List<CourierStockItem> items;
}
