package uz.uwon.pharm.cashRegister;

import uz.uwon.pharm.users.User;

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
}
