package uz.uwon.pharm.cashRegister;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CourierCashView {
    Long getCourierId();
    String getCourierName();
    BigDecimal getOnCourierSum();
    Long getOnCourierCount();
    LocalDateTime getOldestOnCourierAt();
    BigDecimal getHandedSum();
    Long getHandedCount();
    LocalDateTime getLastHandedAt();
}
