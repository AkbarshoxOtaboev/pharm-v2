package uz.uwon.pharm.courierStock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.uwon.pharm.courierStockHistory.CourierStockHistory;
import uz.uwon.pharm.courierStockHistory.CourierStockHistoryRepository;
import uz.uwon.pharm.courierStockHistory.CourierStockHistoryResponse;
import uz.uwon.pharm.courierStockHistory.StockActionType;
import uz.uwon.pharm.exceptions.NotFoundException;
import uz.uwon.pharm.product.Product;
import uz.uwon.pharm.product.ProductRepository;
import uz.uwon.pharm.store.Store;
import uz.uwon.pharm.store.StoreRepository;
import uz.uwon.pharm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierStockServiceImplement implements CourierStockService{
    private final CourierStockRepository repository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final CourierStockHistoryRepository historyRepository;

    @Override
    public void save(CourierStockDTO dto) {
        log.info("Transfer product from stock to courier stock courier id {}", dto.getCourierId());

        for (CourierStockItem item : dto.getItems()) {

            Store store = storeRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            // ❗️ SECURITY CHECK (остаток на складе)
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
    public List<CourierStockResponse> courierStocks(Long courierId) {
        return repository.findCourierStockByCourierIdOrderByIdAsc(courierId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public void returnToStore(ReturnDTO dto) {
        CourierStock stock = repository
                .findByCourierIdAndProductId(dto.getCourierId(), dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден у курьера"));

        // ❗ проверка
        if (stock.getQuantity().compareTo(dto.getQuantity()) < 0) {
            throw new RuntimeException("Недостаточно товара у курьера");
        }
        BigDecimal before = stock.getQuantity();
        // 🔻 уменьшаем у курьера
        stock.setQuantity(stock.getQuantity().subtract(dto.getQuantity()));
        stock.setTotalAmount(
                stock.getQuantity().multiply(stock.getProductPrice())
        );
        repository.save(stock);
        // ✅ HISTORY
        saveHistory(stock, before, dto.getQuantity().negate(), StockActionType.RETURN, "Возврать к складу");
        // 🔼 увеличиваем склад
        Store store = storeRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден на складе"));

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
        Product product = productRepository.findProductByIdAndStatus(stock.getProductId(), Status.ACTIVE).orElseThrow();
        return new CourierStockResponse(
                stock.getId(),
                stock.getProductId(),
                stock.getProductName(),
                stock.getQuantity(),
                product.getPriceCost(),
                stock.getTotalAmount(),
                stock.getUnitType()
        );
    }

    public CourierStockHistoryResponse toResponse(CourierStockHistory history) {
        return new CourierStockHistoryResponse(
//                history.getId(),
//                history.getCourierId(),
//                history.getProductId(),
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
