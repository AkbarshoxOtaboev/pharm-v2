package uz.uwon.pharm.cashRegister;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.users.UserRepository;
import uz.uwon.pharm.utils.Status;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CashRegisterServiceImplement implements CashRegisterService  {

    private final CashRegisterRepository repository;
    private final UserRepository userRepository;

    @Override
    public void save(CashRegisterDTO dto) {
        User courier = userRepository.findById(dto.getCourierId()).orElseThrow();
        CashRegister cash = CashRegister.builder()
                .courier(courier)
                .orderId(dto.getOrderId())
                .orderTotalSum(dto.getOrderTotalSum())
                .registerStatus(CashRegisterStatus.ON_COURIER)
                .comment(dto.getComment())
                .status(Status.ACTIVE)
                .build();

        repository.save(cash);
    }

    @Override
    public List<CashRegisterResponse> findCashRegistersByCourierId(
            Long courierId,
            String dateFrom,
            String dateTo
    ) {
        User courier = userRepository.findById(courierId).orElseThrow();

        LocalDateTime from = null;
        LocalDateTime to = null;

        if (dateFrom != null && !dateFrom.isEmpty()) {
            from = LocalDate.parse(dateFrom).atStartOfDay();
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
        }

        return repository.findByCourierAndCreatedAtBetweenOrderByIdDesc(courier, from, to)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CashRegisterResponse> fetchMonthlyReport(Long courierId, LocalDateTime dateFrom, LocalDateTime dateTo) {
        User courier = userRepository.findById(courierId).orElseThrow();
        return repository.findByCourierAndCreatedAtBetweenOrderByIdDesc(courier, dateFrom, dateTo)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void returnCash(Long cashId, String comment) {
        CashRegister register = repository.findById(cashId).orElseThrow();
        register.setDateTime(LocalDateTime.now());
        register.setRegisterStatus(CashRegisterStatus.ON_ADMIN);
        register.setComment(comment);
    }

    @Override
    public List<DailyCashDTO> getReports(LocalDate from,LocalDate to) {
        return repository.getDailyCash(from, to);
    }

    @Override
    public BigDecimal totalCourierCash(Long courierId) {
        User user = userRepository.findById(courierId).orElseThrow();
        LocalDate now = LocalDate.now();

        LocalDateTime start = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);

        return repository.totalCourierCash(
                user,
                start,
                end
        );
    }

    private CashRegisterResponse mapToResponse(CashRegister register){
        return new CashRegisterResponse(
                register.getId(),
                register.getCourier().getFullName(),
                register.getOrderId(),
                register.getOrderTotalSum(),
                register.getRegisterStatus(),
                register.getCreatedAt(),
                register.getDateTime(),
                register.getComment()
        );
    }
}
