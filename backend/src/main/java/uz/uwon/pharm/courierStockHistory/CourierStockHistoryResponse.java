package uz.uwon.pharm.courierStockHistory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CourierStockHistoryResponse {

    private Long id;

    private Long courierId;

    @Setter
    private String courierName;

    private String productName;

    private BigDecimal beforeQuantity;

    private BigDecimal changeQuantity;

    private BigDecimal afterQuantity;

    private BigDecimal productPrice;

    private BigDecimal totalAmount;

    private String unitType;

    private String actionType;

    private String comment;

    private LocalDateTime createdAt;
}
