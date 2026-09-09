package uz.uwon.pharm.productSaleLog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import uz.uwon.pharm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.SALE_LOG)
public class SaleLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String productName;
    private BigDecimal productPriceCost;
    private BigDecimal quantity;
    private BigDecimal totalSum;
    private boolean isBonus;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;
    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updatedAt;
}
