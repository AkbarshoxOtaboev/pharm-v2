package uz.uwon.pharm.customer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CustomerSalesRowResponse {
    private Long id;
    private String fullName;
    private String phone;
    private long orderCount;
    private long cancelledCount;
    private long deliveredCount;
    private BigDecimal orderSum;
}
