package uz.uwon.pharm.store;

import java.util.List;

public interface StoreService {
    void saveArrivalProduct(Long storeId, StoreDTO dto);
    StoreResponse findById(Long id);
    List<StoreResponse> findAll();
    void updateArrivalProduct(Long id, StoreDTO dto);
    StoreStatisticsResponse getStoreStatistics();
}
