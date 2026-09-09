package uz.uwon.pharm.productSaleLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
@Builder
public class SaleLogDTO {
    private Long orderId;
    private Long categoryId;
    private String categoryName;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String productName;
    private BigDecimal productPriceCost;
    private BigDecimal quantity;
    private BigDecimal totalSum;
    private boolean isBonus;
}
