package uz.uwon.pharm.storeHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.utils.Status;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class StoreHistoryServiceImplement implements StoreHistoryService {

    private final StoreHistoryRepository repository;


    @Override
    public List<StoreHistoryResponse> findAll() {
        return repository.findAllByStatus(Status.ACTIVE)
                .stream()
                .map(this::mapToStoreHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoreHistoryResponse> getFiltered(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return repository.findAllByStatusAndDateOfArrivalBetween(Status.ACTIVE, startDate, endDate)
                    .stream()
                    .map(this::mapToStoreHistoryResponse)
                    .collect(Collectors.toList());
        }
        return findAll();
    }

    private StoreHistoryResponse mapToStoreHistoryResponse(StoreHistory history){
        return new StoreHistoryResponse(
                history.getId(),
                history.getProductName(),
                history.getQuantity(),
                history.getPriceCost(),
                history.getTotalAmount(),
                history.getDateOfArrival(),
                history.getComment(),
                history.getStatus()
        );
    }
}
