package uz.uwon.pharm.storeHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.common.PageRequests;
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

    @Override
    @Transactional(readOnly = true)
    public Page<StoreHistoryResponse> getPage(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDate start = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.of(9999, 12, 31);
        Sort sort = Sort.by(Sort.Direction.DESC, "dateOfArrival").and(Sort.by(Sort.Direction.DESC, "id"));
        return repository.findAllByStatusAndDateOfArrivalBetween(Status.ACTIVE, start, end, PageRequests.of(page, size, sort))
                .map(this::mapToStoreHistoryResponse);
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
