package uz.uwon.pharm.cashRegister;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class CashRegisterResponse {
    private Long id;
    private String courierName;
    private Long orderId;
    private BigDecimal orderTotalSum;
    private CashRegisterStatus registerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime dateTime;
    private String comment;
}
