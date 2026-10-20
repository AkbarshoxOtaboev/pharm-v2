package uz.uwon.pharm.cashRegister;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.common.PageRequests;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.users.UserRepository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CashRegisterServiceImplement implements CashRegisterService  {

    private static final LocalDateTime MIN_DATE = LocalDate.of(1970, 1, 1).atStartOfDay();
    private static final LocalDateTime MAX_DATE = LocalDate.of(9999, 12, 31).atStartOfDay();

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

        LocalDateTime from = MIN_DATE;
        LocalDateTime to = MAX_DATE;

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
        CashRegister register = repository.findById(cashId)
                .orElseThrow(() -> new NotFoundException("Cash register not found"));
        if (register.getRegisterStatus() == CashRegisterStatus.ON_ADMIN) {
            return;
        }
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

    @Override
    @Transactional(readOnly = true)
    public CashSummaryResponse summary(LocalDate from, LocalDate to, Long courierId) {
        LocalDateTime start = from != null ? from.atStartOfDay() : MIN_DATE;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : MAX_DATE;

        List<CashSummaryResponse.CourierCash> couriers = new ArrayList<>();
        BigDecimal onCourierSum = BigDecimal.ZERO;
        BigDecimal handedSum = BigDecimal.ZERO;
        long onCourierCount = 0;
        long handedCount = 0;

        for (CourierCashView v : repository.courierBalances(start, end)) {
            if (courierId != null && !courierId.equals(v.getCourierId())) continue;
            CashSummaryResponse.CourierCash row = new CashSummaryResponse.CourierCash(
                    v.getCourierId(),
                    v.getCourierName(),
                    nz(v.getOnCourierSum()),
                    nz(v.getOnCourierCount()),
                    v.getOldestOnCourierAt(),
                    nz(v.getHandedSum()),
                    nz(v.getHandedCount()),
                    v.getLastHandedAt()
            );
            couriers.add(row);
            onCourierSum = onCourierSum.add(row.getOnCourierSum());
            onCourierCount += row.getOnCourierCount();
            handedSum = handedSum.add(row.getHandedSum());
            handedCount += row.getHandedCount();
        }
        couriers.sort(Comparator.comparing(CashSummaryResponse.CourierCash::getOnCourierSum).reversed()
                .thenComparing(CashSummaryResponse.CourierCash::getHandedSum, Comparator.reverseOrder()));

        return new CashSummaryResponse(onCourierSum, onCourierCount, handedSum, handedCount, couriers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DailyCashDTO> dailyPage(LocalDate from, LocalDate to, int page, int size) {
        LocalDateTime start = from != null ? from.atStartOfDay() : MIN_DATE;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : MAX_DATE;
        return repository.dailyCashPage(start, end, PageRequests.of(page, size, Sort.unsorted()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CashRegisterResponse> page(Long courierId, CashRegisterStatus status,
                                           LocalDate from, LocalDate to, int page, int size) {
        // Handed-over entries are filtered by the handover time, everything else by creation time.
        String dateField = status == CashRegisterStatus.ON_ADMIN ? "dateTime" : "createdAt";
        Specification<CashRegister> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
            if (courierId != null) predicates.add(cb.equal(root.get("courier").get("id"), courierId));
            if (status != null) predicates.add(cb.equal(root.get("registerStatus"), status));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get(dateField), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(root.get(dateField), to.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::mapToResponse);
    }

    @Override
    public BigDecimal returnAll(Long courierId, String comment) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal sum = BigDecimal.ZERO;
        for (CashRegister register : repository.findByCourierIdAndRegisterStatus(courierId, CashRegisterStatus.ON_COURIER)) {
            if (register.getStatus() != Status.ACTIVE) continue;
            register.setRegisterStatus(CashRegisterStatus.ON_ADMIN);
            register.setDateTime(now);
            if (comment != null && !comment.isBlank()) register.setComment(comment);
            sum = sum.add(nz(register.getOrderTotalSum()));
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
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
                register.getComment(),
                register.getCourier().getId()
        );
    }
}
