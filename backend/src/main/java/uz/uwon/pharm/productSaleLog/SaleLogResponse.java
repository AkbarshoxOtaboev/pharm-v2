package uz.uwon.pharm.productSaleLog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class SaleLogResponse {
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
