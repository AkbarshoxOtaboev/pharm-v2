package uz.uwon.pharm.order;

import java.math.BigDecimal;

public interface MonthlyStatDTO {
    Integer getMonth();
    BigDecimal getTotalSum();
}
