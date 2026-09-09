package uz.uwon.pharm.users;

import java.math.BigDecimal;

public interface CourierMonthlyStatProjection {
    Long getCourierId();
    String getCourierName();

    Long getAllOrders();
    Long getDeliveredOrders();
    Long getCancelledOrders();

    BigDecimal getDeliveredTotalSum();
}
