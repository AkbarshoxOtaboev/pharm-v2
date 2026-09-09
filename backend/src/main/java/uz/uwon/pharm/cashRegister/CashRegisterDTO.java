package uz.uwon.pharm.cashRegister;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CashRegisterDTO {
    private Long courierId;
    private Long orderId;
    private BigDecimal orderTotalSum;
    private String comment;

}
