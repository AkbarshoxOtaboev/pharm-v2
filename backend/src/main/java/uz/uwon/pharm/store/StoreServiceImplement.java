package uz.uwon.pharm.store;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.product.Product;
import uz.uwon.pharm.product.ProductRepository;
import uz.uwon.pharm.storeHistory.StoreHistory;
import uz.uwon.pharm.storeHistory.StoreHistoryRepository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class StoreServiceImplement implements StoreService {

    private final StoreRepository repository;
    private final StoreHistoryRepository historyRepository;

    @Override
    public void saveArrivalProduct(Long storeId, StoreDTO dto) {
        log.info("Finding store by id {}", storeId);
        Store store = repository.findByIdAndStatus(storeId, Status.ACTIVE).orElseThrow(()->new NotFoundException("Store not found with id " + storeId));
        BigDecimal quantity = store.getQuantity().add(dto.getQuantity());
        BigDecimal totalAmount = store.getTotalAmount().add(dto.getQuantity().multiply(store.getProduct().getPrice()));
        store.setQuantity(quantity);
        store.setTotalAmount(totalAmount);
        store.setDateOfArrival(dto.getDateOfArrival());

        StoreHistory history = StoreHistory.builder()
                .productName(store.getProduct().getName())
                .quantity(dto.getQuantity())
                .priceCost(store.getProduct().getPrice())
                .totalAmount(dto.getQuantity().multiply(store.getProduct().getPrice()))
                .dateOfArrival(dto.getDateOfArrival())
                .comment(dto.getComment())
                .status(Status.ACTIVE)
                .build();
        historyRepository.save(history);
    }

    @Override
    public StoreResponse findById(Long id) {
        log.info("Find store by id {} and map to store response", id);
        return mapToStoreResponse(repository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(()->new NotFoundException("Store not found")));
    }

    @Override
    public List<StoreResponse> findAll() {
        log.info("Fetching all active stores");
        return repository.findAllByStatusOrderByIdAsc(Status.ACTIVE)
                .stream()
                .map(this::mapToStoreResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void updateArrivalProduct(Long id, StoreDTO dto) {

    }

    @Override
    public StoreStatisticsResponse getStoreStatistics() {
        return repository.getStoreStatistics();
    }

    private StoreResponse mapToStoreResponse(Store store){
        return new StoreResponse(
                store.getId(),
                store.getProduct().getName(),
                store.getProduct().getPrice(),
                store.getProduct().getPriceCost(),
                store.getProduct().getUnitType(),
                store.getQuantity(),
                store.getTotalAmount(),
                store.getDateOfArrival(),
                store.getStatus()
        );
    }
}
