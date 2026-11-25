package uz.uwon.pharm.order.export;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record OrderExportRow(
        String customerName,
        String address,
        String phoneDigits,
        int orderCount,
        Map<String, BigDecimal> items,
        Map<String, BigDecimal> gifts,
        String note,
        String specialist
) {
    public static OrderExportRow empty() {
        return new OrderExportRow(
                "",
                "",
                "",
                1,
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                "",
                ""
        );
    }
}
