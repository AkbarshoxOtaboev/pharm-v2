package uz.uwon.pharm.store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.uwon.pharm.product.UnitType;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class StoreResponse {
    private Long storeId;
    private String productName;
    private BigDecimal productPrice;
    private BigDecimal productPriceCost;
    private UnitType productUnitType;
    private BigDecimal storeQuantity;
    private BigDecimal storeTotalAmount;
    private LocalDate storeDateOfArrival;
    private Status storeStatus;
}
