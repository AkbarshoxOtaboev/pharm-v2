package uz.uwon.pharm.cashRegister;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CashRegisterService {
    void save(CashRegisterDTO dto);
    List<CashRegisterResponse> findCashRegistersByCourierId(Long courierIdString, String dateFrom,String dateTo);
    List<CashRegisterResponse> fetchMonthlyReport(Long courierId, LocalDateTime dateFrom, LocalDateTime dateTo);
    void returnCash(Long cashId, String comment);

    List<DailyCashDTO> getReports(LocalDate from, LocalDate to);
    BigDecimal totalCourierCash(Long courierId);

    CashSummaryResponse summary(LocalDate from, LocalDate to, Long courierId);

    Page<DailyCashDTO> dailyPage(LocalDate from, LocalDate to, int page, int size);

    Page<CashRegisterResponse> page(Long courierId, CashRegisterStatus status,
                                    LocalDate from, LocalDate to, int page, int size);

    /** Marks every ON_COURIER entry of the courier as handed over; returns the handed sum. */
    BigDecimal returnAll(Long courierId, String comment);
}
