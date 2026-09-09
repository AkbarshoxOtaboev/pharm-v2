package uz.uwon.pharm.store;

import java.math.BigDecimal;

public interface StoreStatisticsResponse {
    Long getTotalProducts();
    Long getTotalQuantity();
    BigDecimal getTotalCostSum();
    Long getTodayAddedQuantity();
}
