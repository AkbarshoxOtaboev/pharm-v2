package uz.uwon.pharm.productSaleLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleLogService {
    void save(SaleLogDTO dto);
    List<SaleLogResponse> fetchAllSaleList(LocalDate start, LocalDate end);
}
