package uz.uwon.pharm.order;

import java.math.BigDecimal;

public interface OrderStatResponse {
    Long getClientsCount();
    Long getOrdersCount();
    Long getDeliveredOrdersCount();
    Long getCancelledOrdersCount();

    BigDecimal getDailyDeliveredOrdersTotalSum();
    BigDecimal getMonthlyDeliveredOrdersTotalSum();
    BigDecimal getDeliveredOrdersTotalSum();
    BigDecimal getCancelledOrdersTotalSum();
}
