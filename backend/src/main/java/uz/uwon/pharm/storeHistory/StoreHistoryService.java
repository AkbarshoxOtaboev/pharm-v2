package uz.uwon.pharm.storeHistory;

import java.time.LocalDate;
import java.util.List;

public interface StoreHistoryService {
    List<StoreHistoryResponse> findAll();
    List<StoreHistoryResponse> getFiltered(LocalDate startDate, LocalDate endDate);

}
