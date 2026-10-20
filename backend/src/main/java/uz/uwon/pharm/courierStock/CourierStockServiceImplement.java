package uz.uwon.pharm.courierStock;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.common.PageRequests;
import uz.uwon.pharm.courierStockHistory.CourierStockHistory;
import uz.uwon.pharm.courierStockHistory.CourierStockHistoryRepository;
import uz.uwon.pharm.courierStockHistory.CourierStockHistoryResponse;
import uz.uwon.pharm.courierStockHistory.StockActionType;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.order.OrderStatus;
import uz.uwon.pharm.orderitem.OrderItemRepository;
import uz.uwon.pharm.product.Product;
import uz.uwon.pharm.product.ProductRepository;
import uz.uwon.pharm.store.Store;
import uz.uwon.pharm.store.StoreRepository;
import uz.uwon.pharm.users.User;
import uz.uwon.pharm.users.UserRepository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierStockServiceImplement implements CourierStockService{
    private final CourierStockRepository repository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final CourierStockHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public void save(CourierStockDTO dto) {
        log.info("Transfer product from stock to courier stock courier id {}", dto.getCourierId());

        for (CourierStockItem item : dto.getItems()) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }

            Store store = storeRepository.findByProduct_IdAndStatus(item.getProductId(), Status.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            if (store.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw new IllegalArgumentException(
                        "Not enough product in stock. Available: "
                                + store.getQuantity() + ", requested: " + item.getQuantity()
                );
            }

            Optional<CourierStock> existing =
                    repository.findByCourierIdAndProductId(dto.getCourierId(), item.getProductId());

            if (existing.isPresent()) {

                CourierStock stock = existing.get();

                BigDecimal before = stock.getQuantity();

                stock.setQuantity(stock.getQuantity().add(item.getQuantity()));
                stock.setTotalAmount(stock.getQuantity().multiply(stock.getProductPrice()));

                // ✅ HISTORY
                saveHistory(stock, before, item.getQuantity(),
                        StockActionType.INCOME, "Из склада");

            } else {

                CourierStock stock = CourierStock.builder()
                        .courierId(dto.getCourierId())
                        .productId(item.getProductId())
                        .productName(store.getProduct().getName())
                        .productPrice(store.getProduct().getPriceCost())
                        .quantity(item.getQuantity())
                        .totalAmount(item.getQuantity()
                                .multiply(store.getProduct().getPriceCost()))
                        .unitType(store.getProduct().getUnitType())
                        .build();

                repository.save(stock);

                // ✅ HISTORY
                saveHistory(stock, BigDecimal.ZERO, item.getQuantity(),
                        StockActionType.INCOME, "Новый");
            }

            // 📉 списание со склада
            store.setQuantity(store.getQuantity().subtract(item.getQuantity()));
            store.setTotalAmount(
                    store.getQuantity().multiply(store.getProduct().getPrice())
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierStockResponse> courierStocks(Long courierId) {
        Map<Long, OrderItemRepository.ReservedStockView> reserved = orderItemRepository
                .reservedByCourier(courierId, OrderStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(OrderItemRepository.ReservedStockView::getProductId, v -> v, (a, b) -> a));

        return repository.findCourierStockByCourierIdOrderByIdAsc(courierId).stream()
                .map(stock -> {
                    CourierStockResponse response = mapToResponse(stock);
                    OrderItemRepository.ReservedStockView r = reserved.get(stock.getProductId());
                    if (r != null) {
                        response.setInOrders(r.getQuantity());
                        response.setInOrdersCount(r.getOrdersCount());
                    }
                    return response;
                })
                .toList();
    }

    @Override
    public void returnToStore(ReturnDTO dto) {
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        CourierStock stock = repository
                .findByCourierIdAndProductId(dto.getCourierId(), dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found on courier"));

        if (stock.getQuantity().compareTo(dto.getQuantity()) < 0) {
            throw new IllegalArgumentException("Not enough product on courier");
        }
        BigDecimal before = stock.getQuantity();
        stock.setQuantity(stock.getQuantity().subtract(dto.getQuantity()));
        stock.setTotalAmount(
                stock.getQuantity().multiply(stock.getProductPrice())
        );
        repository.save(stock);
        saveHistory(stock, before, dto.getQuantity().negate(), StockActionType.RETURN, "Возврать к складу");
        Store store = storeRepository.findByProduct_IdAndStatus(dto.getProductId(), Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found in store"));

        store.setQuantity(store.getQuantity().add(dto.getQuantity()));
        store.setTotalAmount(
                store.getQuantity().multiply(store.getProduct().getPrice())
        );
    }

    @Override
    public List<CourierStockResponse> productSearch(Long courierId, String name) {
        BigDecimal quantity = BigDecimal.ZERO;
        return repository.findByCourierIdAndProductNameContainingIgnoreCaseAndQuantityGreaterThan(courierId, name, quantity)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CourierStockHistoryResponse> stockHistoryResponse(
            Long courierId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        LocalDate now = LocalDate.now();

        // 👉 если даты null → текущий месяц
        if (startDate == null && endDate == null) {
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        return historyRepository
                .findByCourierIdAndCreatedAtBetweenOrderByIdDesc(courierId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierStockHistoryResponse> historyPage(Long courierId, StockActionType actionType,
                                                         LocalDate startDate, LocalDate endDate,
                                                         int page, int size) {
        Specification<CourierStockHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (courierId != null) predicates.add(cb.equal(root.get("courierId"), courierId));
            if (actionType != null) predicates.add(cb.equal(root.get("actionType"), actionType));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            if (endDate != null) predicates.add(cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<CourierStockHistoryResponse> result = historyRepository
                .findAll(spec, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toResponse);

        Set<Long> courierIds = result.stream()
                .map(CourierStockHistoryResponse::getCourierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = userRepository.findAllById(courierIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
        result.forEach(r -> r.setCourierName(names.get(r.getCourierId())));
        return result;
    }

    private void saveHistory(CourierStock stock,
                             BigDecimal before,
                             BigDecimal change,
                             StockActionType type,
                             String comment) {

        BigDecimal after = before.add(change);

        CourierStockHistory history = CourierStockHistory.builder()
                .courierId(stock.getCourierId())
                .productId(stock.getProductId())
                .productName(stock.getProductName())
                .beforeQuantity(before)
                .changeQuantity(change)
                .afterQuantity(after)
                .productPrice(stock.getProductPrice())
                .totalAmount(stock.getProductPrice().multiply(change))
                .unitType(stock.getUnitType())
                .actionType(type)
                .comment(comment)
                .build();

        historyRepository.save(history);
    }

    private CourierStockResponse mapToResponse(CourierStock stock){
        BigDecimal price = productRepository.findProductByIdAndStatus(stock.getProductId(), Status.ACTIVE)
                .map(Product::getPriceCost)
                .orElse(stock.getProductPrice());
        BigDecimal quantity = stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO;
        return new CourierStockResponse(
                stock.getId(),
                stock.getProductId(),
                stock.getProductName(),
                quantity,
                price,
                price != null ? quantity.multiply(price) : stock.getTotalAmount(),
                stock.getUnitType(),
                BigDecimal.ZERO,
                0L
        );
    }

    public CourierStockHistoryResponse toResponse(CourierStockHistory history) {
        return new CourierStockHistoryResponse(
                history.getId(),
                history.getCourierId(),
                null,
                history.getProductName(),
                history.getBeforeQuantity(),
                history.getChangeQuantity(),
                history.getAfterQuantity(),
                history.getProductPrice(),
                history.getTotalAmount(),
                history.getUnitType().name(),
                history.getActionType().name(),
                history.getComment(),
                history.getCreatedAt()
        );
    }
}
