package uz.uwon.pharm.customer;

import java.math.BigDecimal;

public interface CustomerSalesRowView {
    Long getId();
    String getFullName();
    String getPhone();
    Long getOrderCount();
    Long getCancelledCount();
    Long getDeliveredCount();
    BigDecimal getOrderSum();
}
