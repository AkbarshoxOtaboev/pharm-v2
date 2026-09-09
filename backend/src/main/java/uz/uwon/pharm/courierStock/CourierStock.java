package uz.uwon.pharm.courierStock;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import uz.uwon.pharm.product.UnitType;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.COURIER_STOCKS)
public class CourierStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long courierId;
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal productPrice;
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private UnitType unitType;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime updateAt;
}
