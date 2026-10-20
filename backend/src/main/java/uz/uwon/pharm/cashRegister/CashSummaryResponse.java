package uz.uwon.pharm.cashRegister;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CashSummaryResponse {
    /** Money currently held by couriers (all time). */
    private BigDecimal onCourierSum;
    private long onCourierCount;
    /** Money handed over to the admin within the requested period. */
    private BigDecimal handedSum;
    private long handedCount;
    private List<CourierCash> couriers;

    @Getter
    @AllArgsConstructor
    public static class CourierCash {
        private Long courierId;
        private String courierName;
        private BigDecimal onCourierSum;
        private long onCourierCount;
        private LocalDateTime oldestOnCourierAt;
        private BigDecimal handedSum;
        private long handedCount;
        private LocalDateTime lastHandedAt;
    }
}
