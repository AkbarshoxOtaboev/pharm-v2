package uz.uwon.pharm.cashRegister;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CourierPeriodCashResponse {
    /** Yetkazilgan buyurtmalar bo'yicha kassaga tushgan jami (davr). */
    private BigDecimal collectedSum;
    /** Shu davr buyurtmalaridan kassaga topshirilgani. */
    private BigDecimal handedSum;
    /** Shu davr buyurtmalaridan hali kuryerda qolgani. */
    private BigDecimal onCourierSum;
}
