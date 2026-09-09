package uz.uwon.pharm.storeHistory;

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
public class StoreHistoryResponse {
    private Long id;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal priceCost;
    private BigDecimal totalAmount;
    private LocalDate dateOfArrival;
    private String comment;
    private Status status;
}
