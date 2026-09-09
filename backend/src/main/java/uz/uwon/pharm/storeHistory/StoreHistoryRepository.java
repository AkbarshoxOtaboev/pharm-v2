package uz.uwon.pharm.storeHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StoreHistoryRepository extends JpaRepository<StoreHistory, Long> {
    List<StoreHistory> findAllByStatus(Status status);

    List<StoreHistory> findAllByStatusAndDateOfArrivalBetween(Status status, LocalDate startDate, LocalDate endDate);
}
