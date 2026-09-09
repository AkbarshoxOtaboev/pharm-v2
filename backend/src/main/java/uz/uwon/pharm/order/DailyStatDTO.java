package uz.uwon.pharm.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyStatDTO {
    LocalDate getDate();

    Long getOrdersCount();
    Long getDeliveredCount();
    Long getCancelledCount();

    BigDecimal getDeliveredSum();
    BigDecimal getCancelledSum();
    BigDecimal getDeliveredBonusSum();
}
