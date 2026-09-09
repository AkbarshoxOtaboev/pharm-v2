package uz.uwon.pharm.courierStockHistory;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import uz.uwon.pharm.product.UnitType;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.COURIER_STOCK_HISTORY)
public class CourierStockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long courierId;

    private Long productId;

    private String productName;

    // 📦 сколько было ДО операции
    private BigDecimal beforeQuantity;

    // 📦 изменение (+ или -)
    private BigDecimal changeQuantity;

    // 📦 сколько стало ПОСЛЕ
    private BigDecimal afterQuantity;

    private BigDecimal productPrice;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private UnitType unitType;

    // 📌 тип операции
    @Enumerated(EnumType.STRING)
    private StockActionType actionType;

    private String comment;

    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;
}