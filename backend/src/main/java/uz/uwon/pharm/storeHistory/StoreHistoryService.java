package uz.uwon.pharm.storeHistory;

import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface StoreHistoryService {
    List<StoreHistoryResponse> findAll();
    List<StoreHistoryResponse> getFiltered(LocalDate startDate, LocalDate endDate);
    Page<StoreHistoryResponse> getPage(LocalDate startDate, LocalDate endDate, int page, int size);

}
