package uz.uwon.pharm.users;

import java.math.BigDecimal;

public interface CourierStatsDto {

    Long getTotalOrders();
    Long getDeliveredCount();
    Long getShippedCount();
    Long getCanceledCount();
    Long getPendingCount();
    BigDecimal getCashTotal();
    BigDecimal getCardTotal();
    BigDecimal getTotalSum();
    BigDecimal getShippedSum();
    BigDecimal getCancelledSum();
    BigDecimal getBonusSum();
}