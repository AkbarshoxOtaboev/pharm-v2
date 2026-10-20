package uz.uwon.pharm.courierStock;

import org.springframework.data.domain.Page;
import uz.uwon.pharm.courierStockHistory.CourierStockHistoryResponse;
import uz.uwon.pharm.courierStockHistory.StockActionType;

import java.time.LocalDate;
import java.util.List;

public interface CourierStockService {
    void save(CourierStockDTO dto);
    List<CourierStockResponse> courierStocks(Long courierId);

    void returnToStore(ReturnDTO dto);

    List<CourierStockResponse> productSearch(Long courierId, String name);

    List<CourierStockHistoryResponse> stockHistoryResponse(Long courierId, LocalDate startDate, LocalDate endDate);

    Page<CourierStockHistoryResponse> historyPage(Long courierId, StockActionType actionType,
                                                  LocalDate startDate, LocalDate endDate, int page, int size);
}
