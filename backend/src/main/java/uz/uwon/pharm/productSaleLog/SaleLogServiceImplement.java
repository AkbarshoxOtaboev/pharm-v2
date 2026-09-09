package uz.uwon.pharm.productSaleLog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleLogServiceImplement implements SaleLogService {

    private final SaleLogRepository repository;

    @Override
    public void save(SaleLogDTO dto) {
        SaleLog log = SaleLog.builder()
                .orderId(dto.getOrderId())
                .categoryId(dto.getCategoryId())
                .categoryName(dto.getCategoryName())
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .customerAddress(dto.getCustomerAddress())
                .productName(dto.getProductName())
                .productPriceCost(dto.getProductPriceCost())
                .quantity(dto.getQuantity())
                .totalSum(dto.getTotalSum())
                .isBonus(dto.isBonus())
                .build();
        repository.save(log);
    }

    @Override
    public List<SaleLogResponse> fetchAllSaleList(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (start != null) {
            startDateTime = start.atStartOfDay();
        }

        if (end != null) {
            endDateTime = end.atTime(23, 59, 59);
        }
        return repository.findAllByCreatedAtBetweenOrderByIdDesc(startDateTime, endDateTime)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SaleLogResponse mapToResponse(SaleLog saleLog){
        return new SaleLogResponse(
                saleLog.getOrderId(),
                saleLog.getCategoryId(),
                saleLog.getCategoryName(),
                saleLog.getCustomerName(),
                saleLog.getCustomerPhone(),
                saleLog.getCustomerAddress(),
                saleLog.getProductName(),
                saleLog.getProductPriceCost(),
                saleLog.getQuantity(),
                saleLog.getTotalSum(),
                saleLog.isBonus(),
                saleLog.getCreatedAt(),
                saleLog.getUpdatedAt()
        );
    }
}
