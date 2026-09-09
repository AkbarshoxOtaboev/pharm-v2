package uz.uwon.pharm.users;

import java.math.BigDecimal;

public interface CourierStatsDto {

    Long getTotalOrders();
    Long getDeliveredCount();
    Long getCanceledCount();
    Long getPendingCount();
    BigDecimal getCashTotal();   // 🔥 sum
    BigDecimal getTotalSum();
}