package uz.uwon.pharm.cashRegister;

import java.math.BigDecimal;

/** Period money for one courier: collected = handed + still on courier. */
public interface CourierPeriodCashView {
    BigDecimal getCollectedSum();
    BigDecimal getHandedSum();
    BigDecimal getOnCourierSum();
}
